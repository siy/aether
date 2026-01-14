package org.pragmatica.aether.config;
/**
 * Configuration for application HTTP server.
 *
 * @param enabled whether the app HTTP server is enabled
 * @param port    base port for app HTTP server (nodes use port, port+1, etc.)
 */
public record AppHttpConfig(boolean enabled,
                            int port) {
    public static final int DEFAULT_APP_HTTP_PORT = 8070;

    public static AppHttpConfig defaults() {
        return new AppHttpConfig(false, DEFAULT_APP_HTTP_PORT);
    }

    public static AppHttpConfig enabledOnDefaultPort() {
        return new AppHttpConfig(true, DEFAULT_APP_HTTP_PORT);
    }

    public static AppHttpConfig enabledOnPort(int port) {
        return new AppHttpConfig(true, port);
    }

    public static AppHttpConfig disabled() {
        return new AppHttpConfig(false, DEFAULT_APP_HTTP_PORT);
    }

    /**
     * Get app HTTP port for a specific node (0-indexed).
     */
    public int portFor(int nodeIndex) {
        return port + nodeIndex;
    }

    public AppHttpConfig withEnabled(boolean enabled) {
        return new AppHttpConfig(enabled, port);
    }

    public AppHttpConfig withPort(int port) {
        return new AppHttpConfig(enabled, port);
    }
}
