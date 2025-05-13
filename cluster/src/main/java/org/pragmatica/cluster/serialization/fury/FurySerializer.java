package org.pragmatica.cluster.serialization.fury;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.apache.fury.Fury;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.config.Language;
import org.apache.fury.io.FuryInputStream;
import org.pragmatica.cluster.serialization.CustomClasses;
import org.pragmatica.cluster.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface FurySerializer extends Serializer {
    static FurySerializer furySerializer() {
        record furySerializer(ThreadSafeFury fury) implements FurySerializer {
            private static final Logger log = LoggerFactory.getLogger(FurySerializer.class);

            @Override
            public <T> void write(ByteBuf byteBuf, T object) {
                try (var outputStream = new ByteBufOutputStream(byteBuf)) {
                    fury().serialize(outputStream, object);
                } catch (Exception e) {
                    log.error("Error serializing object", e);
                    throw new RuntimeException(e);
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T read(ByteBuf byteBuf) {
                try (var stream = new FuryInputStream(new ByteBufInputStream(byteBuf))) {
                    return (T) fury().deserialize(stream);
                } catch (Exception e) {
                    log.error("Error deserializing object", e);
                    throw new RuntimeException(e);
                }
            }
        }

        int coreCount = Runtime.getRuntime().availableProcessors();
        var fury = Fury.builder()
                       .withLanguage(Language.JAVA)
                       .withCodegen(false)
                       .buildThreadSafeFuryPool(coreCount, coreCount * 2);

        CustomClasses.configure(fury::register);

        return new furySerializer(fury);
    }
}
