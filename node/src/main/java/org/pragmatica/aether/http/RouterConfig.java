package org.pragmatica.aether.http;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.net.tcp.TlsConfig;

/**
 * Configuration for the HTTP router.
 *
 * @param port             Port to bind to
 * @param maxContentLength Maximum content length for requests
 * @param tls              TLS configuration (empty for plain HTTP)
 */
public record RouterConfig(int port,
                           int maxContentLength,
                           Option<TlsConfig> tls) {
    public static final int DEFAULT_PORT = 8081;
    public static final int DEFAULT_MAX_CONTENT_LENGTH = 1024 * 1024;

    /**
     * Default configuration with validated defaults.
     */
    public static RouterConfig defaultConfig() {
        return new RouterConfig(DEFAULT_PORT, DEFAULT_MAX_CONTENT_LENGTH, Option.empty());
    }

    /**
     * Create configuration with validated port.
     */
    public static Result<RouterConfig> routerConfig(int port) {
        return routerConfig(port, DEFAULT_MAX_CONTENT_LENGTH);
    }

    /**
     * Create configuration with validated port and max content length.
     */
    public static Result<RouterConfig> routerConfig(int port, int maxContentLength) {
        return validatePort(port)
                           .flatMap(_ -> validateMaxContentLength(maxContentLength))
                           .map(_ -> new RouterConfig(port,
                                                      maxContentLength,
                                                      Option.empty()));
    }

    /**
     * Create a new configuration with TLS enabled.
     */
    public RouterConfig withTls(TlsConfig tlsConfig) {
        return new RouterConfig(port, maxContentLength, Option.some(tlsConfig));
    }

    private static Result<Integer> validatePort(int port) {
        if (port < 1 || port > 65535) {
            return RouterConfigError.invalidPort(port)
                                    .result();
        }
        return Result.success(port);
    }

    private static Result<Integer> validateMaxContentLength(int maxContentLength) {
        if (maxContentLength < 1) {
            return RouterConfigError.invalidMaxContentLength(maxContentLength)
                                    .result();
        }
        return Result.success(maxContentLength);
    }

    /**
     * Router configuration errors.
     */
    public sealed interface RouterConfigError extends Cause {
        record InvalidPort(int port) implements RouterConfigError {
            @Override
            public String message() {
                return "Invalid port " + port + ": must be between 1 and 65535";
            }
        }

        record InvalidMaxContentLength(int length) implements RouterConfigError {
            @Override
            public String message() {
                return "Invalid maxContentLength " + length + ": must be positive";
            }
        }

        static RouterConfigError invalidPort(int port) {
            return new InvalidPort(port);
        }

        static RouterConfigError invalidMaxContentLength(int length) {
            return new InvalidMaxContentLength(length);
        }
    }
}
