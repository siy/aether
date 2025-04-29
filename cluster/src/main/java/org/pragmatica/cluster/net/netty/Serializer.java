package org.pragmatica.cluster.net.netty;

public interface Serializer {
    <T> T decode(byte[] bytes, Class<T> clazz);
    <T> byte[] encode(T object);
}
