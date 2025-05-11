package org.pragmatica.cluster.net.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/// Basic serialization interface
public interface Serializer {
    default <T> T decode(byte[] bytes) {
        return read(Unpooled.wrappedBuffer(bytes));
    }

    default <T> byte[] encode(T object) {
        var byteBuf = Unpooled.buffer();
        write(byteBuf, object);
        return byteBuf.array();
    }

    <T> void write(ByteBuf byteBuf, T object);

    <T> T read(ByteBuf byteBuf);
}
