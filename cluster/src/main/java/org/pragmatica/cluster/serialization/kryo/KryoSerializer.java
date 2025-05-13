package org.pragmatica.cluster.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.pragmatica.cluster.serialization.CustomClasses;
import org.pragmatica.cluster.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface KryoSerializer extends Serializer {
    static KryoSerializer kryoSerializer() {
        record kryoSerializer(Pool<Kryo> pool) implements KryoSerializer {
            private static final Logger log = LoggerFactory.getLogger(KryoSerializer.class);

            @Override
            public <T> void write(ByteBuf byteBuf, T object) {
                var kryo = pool.obtain();

                try (var byteBufOutputStream = new ByteBufOutputStream(byteBuf);
                     var output = new Output(byteBufOutputStream)) {
                    kryo.writeClassAndObject(output, object);
                } catch (Exception e) {
                    log.error("Error serializing object", e);
                    throw new RuntimeException(e);
                } finally {
                    pool.free(kryo);
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T read(ByteBuf byteBuf) {
                var kryo = pool.obtain();

                try (var byteBufInputStream = new ByteBufInputStream(byteBuf);
                     var input = new Input(byteBufInputStream)) {
                    return (T) kryo.readClassAndObject(input);
                } catch (Exception e) {
                    log.error("Error deserializing object", e);
                    throw new RuntimeException(e);
                } finally {
                    pool().free(kryo);
                }
            }
        }

        var pool = new Pool<Kryo>(true, false, Runtime.getRuntime().availableProcessors() * 2) {
            @Override
            protected Kryo create() {
                var kryo = new Kryo();

                CustomClasses.configure(kryo::register);

                return kryo;
            }
        };

        return new kryoSerializer(pool);
    }
}
