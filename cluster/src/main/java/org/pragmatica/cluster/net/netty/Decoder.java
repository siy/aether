package org.pragmatica.cluster.net.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.pragmatica.cluster.net.serializer.Serializer;
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
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }

//        in.markReaderIndex();
//        int length = in.readInt();
//
//        if (in.readableBytes() < length) {
//            in.resetReaderIndex();
//            return;
//        }
//
        try {
            out.add(serializer.read(in));
        } catch (Exception e) {
            log.error("Error decoding message", e);
            ctx.close();
        }
    }
}
