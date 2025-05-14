package org.pragmatica.cluster.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/// Basic serialization interface
public interface Deserializer {
    default <T> T decode(byte[] bytes) {
        return read(Unpooled.wrappedBuffer(bytes));
    }

    <T> T read(ByteBuf byteBuf);
}
