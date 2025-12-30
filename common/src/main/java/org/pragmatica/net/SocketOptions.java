package org.pragmatica.net;
/**
 * Socket-level options for server configuration.
 *
 * @param soBacklog   maximum queue length for incoming connection requests (SO_BACKLOG)
 * @param soKeepalive whether to enable TCP keepalive probes (SO_KEEPALIVE)
 */
public record SocketOptions(int soBacklog, boolean soKeepalive) {
    public static SocketOptions socketOptions(int soBacklog, boolean soKeepalive) {
        return new SocketOptions(soBacklog, soKeepalive);
    }

    public static SocketOptions defaults() {
        return new SocketOptions(128, true);
    }
}
