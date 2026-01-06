package org.pragmatica.aether.invoke;

import org.pragmatica.aether.artifact.Artifact;
import org.pragmatica.aether.artifact.ArtifactBase;
import org.pragmatica.aether.endpoint.EndpointRegistry;
import org.pragmatica.aether.endpoint.EndpointRegistry.Endpoint;
import org.pragmatica.aether.invoke.InvocationMessage.InvokeRequest;
import org.pragmatica.aether.invoke.InvocationMessage.InvokeResponse;
import org.pragmatica.aether.slice.MethodName;
import org.pragmatica.aether.slice.SliceBridge;
import org.pragmatica.aether.slice.SliceInvokerFacade;
import org.pragmatica.aether.update.RollingUpdate;
import org.pragmatica.aether.update.RollingUpdateManager;
import org.pragmatica.consensus.net.ClusterNetwork;
import org.pragmatica.consensus.NodeId;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.messaging.MessageReceiver;
import org.pragmatica.serialization.Deserializer;
import org.pragmatica.serialization.Serializer;
import org.pragmatica.utility.KSUID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.lang.Unit.unit;
import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Client-side component for invoking methods on remote slices.
 *
 * <p>Supports two invocation patterns:
 * <ul>
 *   <li>Fire-and-forget: {@link #invoke(Artifact, MethodName, Object)}</li>
 *   <li>Request-response: {@link #invokeAndWait(Artifact, MethodName, Object, Class)}</li>
 * </ul>
 *
 * <p>Uses the EndpointRegistry to find the target node for a slice,
 * and routes the invocation via the ClusterNetwork.
 */
public interface SliceInvoker extends SliceInvokerFacade {
    /**
     * Implementation of SliceInvokerFacade for use by slices via SliceRuntime.
     * Parses string artifact/method and delegates to typed methods.
     */
    @Override
    default <R> Promise<R> invokeAndWait(String sliceArtifact,
                                         String methodName,
                                         Object request,
                                         Class<R> responseType) {
        return Artifact.artifact(sliceArtifact)
                       .flatMap(artifact -> MethodName.methodName(methodName)
                                                      .map(method -> new ArtifactMethod(artifact, method)))
                       .fold(cause -> cause.promise(),
                             am -> invokeAndWait(am.artifact(),
                                                 am.method(),
                                                 request,
                                                 responseType));
    }

    record ArtifactMethod(Artifact artifact, MethodName method) {}

    /**
     * Fire-and-forget invocation - sends request without waiting for response.
     *
     * @param slice  Target slice artifact
     * @param method Method to invoke
     * @param request Request parameter
     * @return Promise that completes when request is sent
     */
    Promise<Unit> invoke(Artifact slice, MethodName method, Object request);

    /**
     * Request-response invocation - sends request and waits for response.
     *
     * @param slice        Target slice artifact
     * @param method       Method to invoke
     * @param request      Request parameter
     * @param responseType Expected response type
     * @param <R>          Response type
     * @return Promise resolving to response
     */
    <R> Promise<R> invokeAndWait(Artifact slice, MethodName method, Object request, Class<R> responseType);

    /**
     * Request-response invocation with retry for idempotent operations.
     * Uses exponential backoff with the specified number of retries.
     *
     * @param slice        Target slice artifact
     * @param method       Method to invoke
     * @param request      Request parameter
     * @param responseType Expected response type
     * @param maxRetries   Maximum number of retry attempts (0 = no retries)
     * @param <R>          Response type
     * @return Promise resolving to response
     */
    <R> Promise<R> invokeWithRetry(Artifact slice,
                                   MethodName method,
                                   Object request,
                                   Class<R> responseType,
                                   int maxRetries);

    /**
     * Local invocation - invokes a slice on the local node without network round-trip.
     * Used by HTTP router for handling incoming requests.
     *
     * @param slice        Target slice artifact
     * @param method       Method to invoke
     * @param request      Request parameter
     * @param responseType Expected response type
     * @param <R>          Response type
     * @return Promise resolving to response
     */
    <R> Promise<R> invokeLocal(Artifact slice, MethodName method, Object request, Class<R> responseType);

    /**
     * Handle response from remote invocation.
     */
    @MessageReceiver
    void onInvokeResponse(InvokeResponse response);

    /**
     * Stop the invoker and release resources.
     * <p>
     * Shuts down the retry scheduler, cancels pending invocations,
     * and cleans up resources.
     *
     * @return Promise that completes when shutdown is finished
     */
    Promise<Unit> stop();

    /**
     * Get count of pending invocations (for monitoring).
     */
    int pendingCount();

    /**
     * Default timeout for invocations (30 seconds).
     */
    long DEFAULT_TIMEOUT_MS = 30_000;

    /**
     * Default maximum retries.
     */
    int DEFAULT_MAX_RETRIES = 3;

    /**
     * Base delay for exponential backoff (100ms).
     */
    long BASE_RETRY_DELAY_MS = 100;

    /**
     * Create a new SliceInvoker.
     */
    static SliceInvoker sliceInvoker(NodeId self,
                                     ClusterNetwork network,
                                     EndpointRegistry endpointRegistry,
                                     InvocationHandler invocationHandler,
                                     Serializer serializer,
                                     Deserializer deserializer,
                                     RollingUpdateManager rollingUpdateManager) {
        return new SliceInvokerImpl(self,
                                    network,
                                    endpointRegistry,
                                    invocationHandler,
                                    serializer,
                                    deserializer,
                                    DEFAULT_TIMEOUT_MS,
                                    rollingUpdateManager);
    }

    /**
     * Create with custom timeout.
     */
    static SliceInvoker sliceInvoker(NodeId self,
                                     ClusterNetwork network,
                                     EndpointRegistry endpointRegistry,
                                     InvocationHandler invocationHandler,
                                     Serializer serializer,
                                     Deserializer deserializer,
                                     long timeoutMs,
                                     RollingUpdateManager rollingUpdateManager) {
        return new SliceInvokerImpl(self,
                                    network,
                                    endpointRegistry,
                                    invocationHandler,
                                    serializer,
                                    deserializer,
                                    timeoutMs,
                                    rollingUpdateManager);
    }
}

class SliceInvokerImpl implements SliceInvoker {
    private static final Logger log = LoggerFactory.getLogger(SliceInvokerImpl.class);
    private static final Cause NO_ENDPOINT_FOUND = Causes.cause("No endpoint found for slice/method");
    private static final Cause SLICE_NOT_FOUND = Causes.cause("Slice not found locally");
    private static final Cause INVOKER_STOPPED = Causes.cause("SliceInvoker has been stopped");
    private static final long CLEANUP_INTERVAL_MS = 60_000;

    // Clean up stale entries every minute
    private final NodeId self;
    private final ClusterNetwork network;
    private final EndpointRegistry endpointRegistry;
    private final InvocationHandler invocationHandler;
    private final Serializer serializer;
    private final Deserializer deserializer;
    private final long timeoutMs;
    private final ScheduledExecutorService scheduler;
    private final RollingUpdateManager rollingUpdateManager;

    // Pending request-response invocations awaiting responses
    // Maps correlationId -> (promise, createdAtMs)
    private final ConcurrentHashMap<String, PendingInvocation> pendingInvocations = new ConcurrentHashMap<>();

    private volatile boolean stopped = false;

    record PendingInvocation(Promise<Object> promise, long createdAtMs) {}

    SliceInvokerImpl(NodeId self,
                     ClusterNetwork network,
                     EndpointRegistry endpointRegistry,
                     InvocationHandler invocationHandler,
                     Serializer serializer,
                     Deserializer deserializer,
                     long timeoutMs,
                     RollingUpdateManager rollingUpdateManager) {
        this.self = self;
        this.network = network;
        this.endpointRegistry = endpointRegistry;
        this.invocationHandler = invocationHandler;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.timeoutMs = timeoutMs;
        this.rollingUpdateManager = rollingUpdateManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(this::createSchedulerThread);
        // Schedule periodic cleanup of stale pending invocations
        scheduler.scheduleAtFixedRate(this::cleanupStaleInvocations,
                                      CLEANUP_INTERVAL_MS,
                                      CLEANUP_INTERVAL_MS,
                                      TimeUnit.MILLISECONDS);
    }

    private Thread createSchedulerThread(Runnable r) {
        var t = new Thread(r, "slice-invoker-scheduler");
        t.setDaemon(true);
        return t;
    }

    private void cleanupStaleInvocations() {
        var staleThreshold = System.currentTimeMillis() - (timeoutMs * 2);
        pendingInvocations.entrySet()
                          .removeIf(entry -> isStaleAndCleanup(entry, staleThreshold));
    }

    private boolean isStaleAndCleanup(Map.Entry<String, PendingInvocation> entry, long staleThreshold) {
        var pending = entry.getValue();
        if (pending.createdAtMs() < staleThreshold) {
            log.warn("Cleaning up stale pending invocation: {}", entry.getKey());
            pending.promise()
                   .resolve(Causes.cause("Invocation timed out (cleanup)")
                                  .result());
            return true;
        }
        return false;
    }

    private void cancelPendingInvocation(String id, PendingInvocation pending) {
        pending.promise()
               .resolve(INVOKER_STOPPED.result());
    }

    @Override
    public Promise<Unit> stop() {
        if (stopped) {
            return Promise.success(unit());
        }
        stopped = true;
        log.info("Stopping SliceInvoker with {} pending invocations", pendingInvocations.size());
        // Cancel all pending invocations
        pendingInvocations.forEach(this::cancelPendingInvocation);
        pendingInvocations.clear();
        // Shutdown scheduler
        scheduler.shutdown();
        try{
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread()
                  .interrupt();
        }
        log.info("SliceInvoker stopped");
        return Promise.success(unit());
    }

    @Override
    public int pendingCount() {
        return pendingInvocations.size();
    }

    @Override
    public Promise<Unit> invoke(Artifact slice, MethodName method, Object request) {
        return selectEndpoint(slice, method)
                             .flatMap(endpoint -> sendFireAndForget(endpoint, slice, method, request));
    }

    private Promise<Unit> sendFireAndForget(Endpoint endpoint, Artifact slice, MethodName method, Object request) {
        var payload = serializeRequest(request);
        var correlationId = KSUID.ksuid()
                                 .toString();
        var invokeRequest = new InvokeRequest(self, correlationId, slice, method, payload, false);
        network.send(endpoint.nodeId(), invokeRequest);
        log.debug("Sent fire-and-forget invocation to {}: {}.{}", endpoint.nodeId(), slice, method);
        return Promise.success(unit());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Promise<R> invokeAndWait(Artifact slice, MethodName method, Object request, Class<R> responseType) {
        if (stopped) {
            return INVOKER_STOPPED.promise();
        }
        return selectEndpoint(slice, method)
                             .flatMap(endpoint -> sendRequestResponse(endpoint, slice, method, request));
    }

    @SuppressWarnings("unchecked")
    private <R> Promise<R> sendRequestResponse(Endpoint endpoint, Artifact slice, MethodName method, Object request) {
        var payload = serializeRequest(request);
        var correlationId = KSUID.ksuid()
                                 .toString();
        return Promise.promise(pendingPromise -> setupPendingInvocation((Promise<Object>)(Promise< ? >) pendingPromise,
                                                                        correlationId,
                                                                        endpoint,
                                                                        slice,
                                                                        method,
                                                                        payload));
    }

    private void setupPendingInvocation(Promise<Object> pendingPromise,
                                        String correlationId,
                                        Endpoint endpoint,
                                        Artifact slice,
                                        MethodName method,
                                        byte[] payload) {
        var pending = new PendingInvocation(pendingPromise, System.currentTimeMillis());
        pendingInvocations.put(correlationId, pending);
        pendingPromise.timeout(timeSpan(timeoutMs)
                                       .millis())
                      .onResult(_ -> pendingInvocations.remove(correlationId));
        var invokeRequest = new InvokeRequest(self, correlationId, slice, method, payload, true);
        network.send(endpoint.nodeId(), invokeRequest);
        log.debug("Sent request-response invocation to {}: {}.{} [{}]", endpoint.nodeId(), slice, method, correlationId);
    }

    @Override
    public <R> Promise<R> invokeWithRetry(Artifact slice,
                                          MethodName method,
                                          Object request,
                                          Class<R> responseType,
                                          int maxRetries) {
        return invokeWithRetryInternal(slice, method, request, responseType, 0, maxRetries);
    }

    private <R> Promise<R> invokeWithRetryInternal(Artifact slice,
                                                   MethodName method,
                                                   Object request,
                                                   Class<R> responseType,
                                                   int attempt,
                                                   int maxRetries) {
        if (stopped) {
            return INVOKER_STOPPED.promise();
        }
        var ctx = new RetryContext<>(slice, method, request, responseType, attempt, maxRetries);
        return Promise.promise(promise -> executeWithRetry(promise, ctx));
    }

    private record RetryContext<R>(Artifact slice,
                                   MethodName method,
                                   Object request,
                                   Class<R> responseType,
                                   int attempt,
                                   int maxRetries) {}

    private <R> void executeWithRetry(Promise<R> promise, RetryContext<R> ctx) {
        invokeAndWait(ctx.slice, ctx.method, ctx.request, ctx.responseType)
                     .onSuccess(promise::succeed)
                     .onFailure(cause -> handleRetryFailure(promise, ctx, cause));
    }

    private <R> void handleRetryFailure(Promise<R> promise, RetryContext<R> ctx, Cause cause) {
        if (stopped) {
            promise.fail(INVOKER_STOPPED);
            return;
        }
        if (ctx.attempt < ctx.maxRetries) {
            scheduleRetry(promise, ctx, cause);
        } else {
            log.warn("Invocation failed after {} retries: {}.{} - {}",
                     ctx.maxRetries,
                     ctx.slice,
                     ctx.method,
                     cause.message());
            promise.fail(cause);
        }
    }

    private <R> void scheduleRetry(Promise<R> promise, RetryContext<R> ctx, Cause cause) {
        var nextAttempt = ctx.attempt + 1;
        var delayMs = BASE_RETRY_DELAY_MS * (1L<< ctx.attempt);
        log.debug("Invocation failed, scheduling retry {}/{} in {}ms: {}.{} - {}",
                  nextAttempt,
                  ctx.maxRetries,
                  delayMs,
                  ctx.slice,
                  ctx.method,
                  cause.message());
        var nextCtx = new RetryContext<>(ctx.slice,
                                         ctx.method,
                                         ctx.request,
                                         ctx.responseType,
                                         nextAttempt,
                                         ctx.maxRetries);
        scheduler.schedule(() -> executeWithRetry(promise, nextCtx), delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Promise<R> invokeLocal(Artifact slice, MethodName method, Object request, Class<R> responseType) {
        return invocationHandler.getLocalSlice(slice)
                                .fold(() -> SLICE_NOT_FOUND.<R>promise(),
                                      bridge -> invokeViaBridge(bridge, method, request));
    }

    @SuppressWarnings("unchecked")
    private <R> Promise<R> invokeViaBridge(SliceBridge bridge, MethodName method, Object request) {
        var inputBytes = serializer.encode(request);
        return bridge.invoke(method.name(),
                             inputBytes)
                     .map(outputBytes -> (R) deserializer.decode(outputBytes));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onInvokeResponse(InvokeResponse response) {
        var pending = pendingInvocations.remove(response.correlationId());
        if (pending == null) {
            log.warn("Received response for unknown correlationId: {}", response.correlationId());
            return;
        }
        var promise = pending.promise();
        if (response.success()) {
            try{
                var buf = Unpooled.wrappedBuffer(response.payload());
                var result = deserializer.read(buf);
                buf.release();
                // Release the wrapped buffer
                promise.resolve(Result.success(result));
                log.debug("Completed invocation [{}]", response.correlationId());
            } catch (Exception e) {
                promise.resolve(Causes.fromThrowable(e)
                                      .result());
                log.error("Failed to deserialize response [{}]: {}", response.correlationId(), e.getMessage());
            }
        } else {
            var errorMessage = new String(response.payload());
            promise.resolve(new InvocationError(errorMessage).result());
            log.debug("Invocation failed [{}]: {}", response.correlationId(), errorMessage);
        }
    }

    private Promise<Endpoint> selectEndpoint(Artifact slice, MethodName method) {
        // Check if there's an active rolling update for this artifact
        var artifactBase = ArtifactBase.artifactBase(slice.groupId(), slice.artifactId());
        return rollingUpdateManager.getActiveUpdate(artifactBase)
                                   .fold(() -> endpointRegistry.selectEndpoint(slice, method)
                                                               .fold(() -> NO_ENDPOINT_FOUND.promise(),
                                                                     Promise::success),
                                         update -> selectEndpointWithWeightedRouting(slice, artifactBase, method, update));
    }

    private Promise<Endpoint> selectEndpointWithWeightedRouting(Artifact slice,
                                                                ArtifactBase artifactBase,
                                                                MethodName method,
                                                                RollingUpdate update) {
        log.debug("Using weighted routing for {} during rolling update {}", slice, update.updateId());
        return endpointRegistry.selectEndpointWithRouting(artifactBase,
                                                          method,
                                                          update.routing(),
                                                          update.oldVersion(),
                                                          update.newVersion())
                               .fold(() -> NO_ENDPOINT_FOUND.promise(),
                                     Promise::success);
    }

    private byte[] serializeRequest(Object request) {
        var buf = Unpooled.buffer();
        serializer.write(buf, request);
        var bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return bytes;
    }

    /**
     * Error from remote invocation.
     */
    record InvocationError(String errorMessage) implements Cause {
        @Override
        public String message() {
            return errorMessage;
        }
    }
}
