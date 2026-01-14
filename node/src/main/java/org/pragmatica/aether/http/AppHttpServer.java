package org.pragmatica.aether.http;

import org.pragmatica.aether.config.AppHttpConfig;
import org.pragmatica.aether.http.handler.HttpRequestContext;
import org.pragmatica.aether.http.handler.HttpResponseData;
import org.pragmatica.http.routing.HttpStatus;
import org.pragmatica.http.routing.ProblemDetail;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.net.tcp.TlsConfig;
import org.pragmatica.net.tcp.TlsContextFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.lang.Unit.unit;

/**
 * Application HTTP server for cluster-wide HTTP routing.
 *
 * <p>Handles HTTP requests by looking up routes in HttpRouteRegistry
 * and forwarding to slice handlers via SliceInvoker.
 *
 * <p>Separate from ManagementServer for security isolation.
 */
public interface AppHttpServer {
    Promise<Unit> start();

    Promise<Unit> stop();

    Option<Integer> boundPort();

    static AppHttpServer appHttpServer(AppHttpConfig config,
                                       HttpRouteRegistry routeRegistry,
                                       Option<TlsConfig> tls) {
        return new AppHttpServerImpl(config, routeRegistry, tls);
    }
}

class AppHttpServerImpl implements AppHttpServer {
    private static final Logger log = LoggerFactory.getLogger(AppHttpServerImpl.class);
    private static final int MAX_CONTENT_LENGTH = 16 * 1024 * 1024;
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_PROBLEM = "application/problem+json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AppHttpConfig config;
    private final HttpRouteRegistry routeRegistry;
    private final MultiThreadIoEventLoopGroup bossGroup;
    private final MultiThreadIoEventLoopGroup workerGroup;
    private final Option<SslContext> sslContext;
    private final AtomicReference<Channel> serverChannel = new AtomicReference<>();
    private final AtomicReference<Integer> boundPortRef = new AtomicReference<>();

    AppHttpServerImpl(AppHttpConfig config,
                      HttpRouteRegistry routeRegistry,
                      Option<TlsConfig> tls) {
        this.config = config;
        this.routeRegistry = routeRegistry;
        this.bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        this.workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        this.sslContext = tls.map(TlsContextFactory::create)
                             .flatMap(r -> r.option());
    }

    @Override
    public Promise<Unit> start() {
        if (!config.enabled()) {
            log.info("App HTTP server is disabled");
            return Promise.success(unit());
        }
        return Promise.promise(promise -> {
                                   var bootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
                                                                        .channel(NioServerSocketChannel.class)
                                                                        .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                                                                                          ChannelPipeline p = ch.pipeline();
                                                                                          sslContext.onPresent(ctx -> p.addLast(ctx.newHandler(ch.alloc())));
                                                                                          p.addLast(new HttpServerCodec());
                                                                                          p.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
                                                                                          p.addLast(new AppHttpRequestHandler(routeRegistry));
                                                                                      }
        });
                                   bootstrap.bind(config.port())
                                            .addListener(future -> {
                                                             if (future.isSuccess()) {
                                                                 var channel = ((io.netty.channel.ChannelFuture) future).channel();
                                                                 serverChannel.set(channel);
                                                                 var addr = (java.net.InetSocketAddress) channel.localAddress();
                                                                 boundPortRef.set(addr.getPort());
                                                                 var protocol = sslContext.isPresent()
                                                                                ? "HTTPS"
                                                                                : "HTTP";
                                                                 log.info("{} app server started on port {}",
                                                                          protocol,
                                                                          addr.getPort());
                                                                 promise.succeed(unit());
                                                             } else {
                                                                 log.error("Failed to start app HTTP server on port {}",
                                                                           config.port(),
                                                                           future.cause());
                                                                 promise.fail(Causes.fromThrowable(future.cause()));
                                                             }
                                                         });
                               });
    }

    @Override
    public Promise<Unit> stop() {
        return Promise.promise(promise -> {
                                   var channel = serverChannel.get();
                                   if (channel != null) {
                                       channel.close()
                                              .addListener(_ -> shutdownEventLoops(promise));
                                   } else {
                                       shutdownEventLoops(promise);
                                   }
                               });
    }

    @Override
    public Option<Integer> boundPort() {
        return Option.option(boundPortRef.get());
    }

    private void shutdownEventLoops(Promise<Unit> promise) {
        var bossFuture = bossGroup.shutdownGracefully();
        var workerFuture = workerGroup.shutdownGracefully();
        bossFuture.addListener(_ -> workerFuture.addListener(_ -> {
                                                                 log.info("App HTTP server stopped");
                                                                 promise.succeed(unit());
                                                             }));
    }
}

class AppHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger log = LoggerFactory.getLogger(AppHttpRequestHandler.class);
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_PROBLEM = "application/problem+json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpRouteRegistry routeRegistry;

    AppHttpRequestHandler(HttpRouteRegistry routeRegistry) {
        this.routeRegistry = routeRegistry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult()
                    .isSuccess()) {
            sendProblem(ctx, HttpStatus.BAD_REQUEST, "Invalid HTTP request", request.uri());
            return;
        }
        var method = request.method()
                            .name();
        var uri = request.uri();
        var requestId = UUID.randomUUID()
                            .toString();
        log.debug("Received {} {} [{}]", method, uri, requestId);
        // Parse URI to extract path and query params
        var queryDecoder = new QueryStringDecoder(uri);
        var path = queryDecoder.path();
        var queryParams = convertQueryParams(queryDecoder.parameters());
        // Look up route
        routeRegistry.findRoute(method, path)
                     .onPresent(route -> handleRouteFound(ctx, request, route, path, queryParams, requestId))
                     .onEmpty(() -> sendProblem(ctx,
                                                HttpStatus.NOT_FOUND,
                                                "No route found for " + method + " " + path,
                                                path,
                                                requestId));
    }

    private void handleRouteFound(ChannelHandlerContext ctx,
                                  FullHttpRequest request,
                                  HttpRouteRegistry.RouteInfo route,
                                  String path,
                                  Map<String, List<String>> queryParams,
                                  String requestId) {
        // Build HttpRequestContext
        var headers = convertHeaders(request.headers());
        var body = new byte[request.content()
                                   .readableBytes()];
        request.content()
               .readBytes(body);
        var httpRequestContext = new HttpRequestContext(path,
                                                        request.method()
                                                               .name(),
                                                        queryParams,
                                                        headers,
                                                        body,
                                                        requestId);
        // For now, we just return 503 Service Unavailable
        // SliceInvoker integration will be added in Phase 5
        // This is a stub implementation to complete Phase 4
        log.debug("Route found: {} {} -> {}:{} [{}]",
                  route.httpMethod(),
                  route.pathPrefix(),
                  route.artifact(),
                  route.sliceMethod(),
                  requestId);
        sendProblem(ctx, HttpStatus.SERVICE_UNAVAILABLE, "SliceInvoker integration pending (Phase 5)", path, requestId);
    }

    private Map<String, List<String>> convertQueryParams(Map<String, List<String>> nettyParams) {
        var result = new HashMap<String, List<String>>();
        for (var entry : nettyParams.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    private Map<String, List<String>> convertHeaders(io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
        var result = new HashMap<String, List<String>>();
        for (var name : nettyHeaders.names()) {
            result.put(name, nettyHeaders.getAll(name));
        }
        return result;
    }

    private void sendProblem(ChannelHandlerContext ctx, HttpStatus status, String detail, String instance) {
        sendProblem(ctx,
                    status,
                    detail,
                    instance,
                    UUID.randomUUID()
                        .toString());
    }

    private void sendProblem(ChannelHandlerContext ctx,
                             HttpStatus status,
                             String detail,
                             String instance,
                             String requestId) {
        var problem = ProblemDetail.problemDetail(status, detail, instance, requestId);
        try{
            var json = OBJECT_MAPPER.writeValueAsString(problem);
            var buf = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);
            var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                                       HttpResponseStatus.valueOf(status.code()),
                                                       buf);
            response.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE_PROBLEM);
            response.headers()
                    .setInt(HttpHeaderNames.CONTENT_LENGTH,
                            buf.readableBytes());
            ctx.writeAndFlush(response)
               .addListener(ChannelFutureListener.CLOSE);
        } catch (Exception e) {
            log.error("Failed to serialize ProblemDetail", e);
            sendPlainError(ctx, status);
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpResponseData responseData, String requestId) {
        var buf = Unpooled.wrappedBuffer(responseData.body());
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                                   HttpResponseStatus.valueOf(responseData.statusCode()),
                                                   buf);
        for (var entry : responseData.headers()
                                     .entrySet()) {
            response.headers()
                    .set(entry.getKey(),
                         entry.getValue());
        }
        response.headers()
                .setInt(HttpHeaderNames.CONTENT_LENGTH,
                        buf.readableBytes());
        ctx.writeAndFlush(response)
           .addListener(ChannelFutureListener.CLOSE);
    }

    private void sendPlainError(ChannelHandlerContext ctx, HttpStatus status) {
        var content = "{\"error\":\"" + status.message() + "\"}";
        var buf = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                                   HttpResponseStatus.valueOf(status.code()),
                                                   buf);
        response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE_JSON);
        response.headers()
                .setInt(HttpHeaderNames.CONTENT_LENGTH,
                        buf.readableBytes());
        ctx.writeAndFlush(response)
           .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error handling HTTP request", cause);
        ctx.close();
    }
}
