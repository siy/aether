package org.pragmatica.net;

import org.pragmatica.lang.Option;
import org.pragmatica.net.tcp.TlsConfig;

/**
 * Configuration for TCP server.
 *
 * @param name          server name for logging
 * @param port          port to bind to
 * @param tls           optional TLS configuration for secure connections
 * @param socketOptions socket-level options
 */
public record ServerConfig(
 String name,
 int port,
 Option<TlsConfig> tls,
 SocketOptions socketOptions) {
    public static ServerConfig serverConfig(String name, int port) {
        return new ServerConfig(name, port, Option.empty(), SocketOptions.defaults());
    }

    public static ServerConfig serverConfig(String name, int port, TlsConfig tls) {
        return new ServerConfig(name, port, Option.some(tls), SocketOptions.defaults());
    }

    public ServerConfig withTls(TlsConfig tls) {
        return new ServerConfig(name, port, Option.some(tls), socketOptions);
    }

    public ServerConfig withSocketOptions(SocketOptions socketOptions) {
        return new ServerConfig(name, port, tls, socketOptions);
    }
}
