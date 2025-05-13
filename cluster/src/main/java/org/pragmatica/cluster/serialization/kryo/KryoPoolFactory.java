package org.pragmatica.cluster.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;
import org.pragmatica.cluster.serialization.CustomClasses;

public sealed interface KryoPoolFactory {

    static Pool<Kryo> kryoPool() {
        return new Pool<>(true, false, Runtime.getRuntime().availableProcessors() * 2) {
            @Override
            protected Kryo create() {
                var kryo = new Kryo();

                CustomClasses.configure(kryo::register);

                return kryo;
            }
        };
    }

    @SuppressWarnings("unused")
    record unused() implements KryoPoolFactory {}
}
