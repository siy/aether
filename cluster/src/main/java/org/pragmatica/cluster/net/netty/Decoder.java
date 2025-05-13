package org.pragmatica.cluster.net.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.pragmatica.cluster.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Decoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(Decoder.class);

    private final Serializer serializer;

    public Decoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected synchronized void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }

        try {
            Object read = serializer.read(in);

            if (read == null) {
                log.error("Null object received!!!");
            } else {
            out.add(read);
            }

        } catch (Exception e) {
            log.error("Error decoding message", e);
            ctx.close();
        }
    }
}
