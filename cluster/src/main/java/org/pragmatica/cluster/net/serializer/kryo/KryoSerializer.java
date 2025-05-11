package org.pragmatica.cluster.net.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.pragmatica.cluster.consensus.rabia.*;
import org.pragmatica.cluster.net.serializer.Serializer;
import org.pragmatica.cluster.state.kvstore.KVCommand;
import org.pragmatica.utility.HierarchyScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

import static org.pragmatica.cluster.consensus.rabia.BatchId.randomBatchId;
import static org.pragmatica.cluster.consensus.rabia.CorrelationId.randomCorrelationId;
import static org.pragmatica.cluster.net.NodeId.randomNodeId;

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

                HierarchyScanner.concreteSubtypes(RabiaProtocolMessage.class)
                                .forEach(kryo::register);
                HierarchyScanner.concreteSubtypes(KVCommand.class)
                                .forEach(kryo::register);
                kryo.register(HashMap.class);
                kryo.register(RabiaPersistence.SavedState.empty().getClass());
                kryo.register(randomNodeId().getClass());
                kryo.register(randomBatchId().getClass());
                kryo.register(randomCorrelationId().getClass());
                kryo.register(Phase.class);
                kryo.register(Batch.class);
                kryo.register(StateValue.class);
                kryo.register(byte[].class);
                kryo.register(List.of().getClass());
                kryo.register(List.of(1).getClass());
                kryo.register(List.of(1, 2, 3).getClass());

                kryo.register(RabiaProtocolMessage.class);
                kryo.register(RabiaProtocolMessage.Synchronous.class);
                kryo.register(RabiaProtocolMessage.Asynchronous.class);

                return kryo;
            }
        };

        return new kryoSerializer(pool);
    }
}
