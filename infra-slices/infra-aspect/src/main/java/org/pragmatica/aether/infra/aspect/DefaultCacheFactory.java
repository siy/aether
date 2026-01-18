package org.pragmatica.aether.infra.aspect;

import org.pragmatica.aether.infra.InfraSliceError;
import org.pragmatica.aether.slice.serialization.SerializerFactory;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.time.Duration;
import java.util.Objects;

/**
 * Default implementation of {@link CacheFactory}.
 * Creates typed caches backed by {@link CacheStorage}.
 */
final class DefaultCacheFactory implements CacheFactory {
    private final CacheStorage storage;
    private final SerializerFactory serializerFactory;

    DefaultCacheFactory(CacheStorage storage, SerializerFactory serializerFactory) {
        this.storage = Objects.requireNonNull(storage, "storage must not be null");
        this.serializerFactory = Objects.requireNonNull(serializerFactory, "serializerFactory must not be null");
    }

    @Override
    public <K, V> Cache<K, V> create(CacheConfig<K, V> config) {
        Objects.requireNonNull(config, "config must not be null");
        return new TypedCache<>(config, storage, serializerFactory);
    }

    /**
     * Typed cache implementation using CacheStorage for persistence.
     */
    private record TypedCache<K, V>(CacheConfig<K, V> config,
                                    CacheStorage storage,
                                    SerializerFactory serializerFactory) implements Cache<K, V> {
        @Override
        public Promise<Option<V>> get(K key) {
            return serializeKey(key)
                               .map(storage::get)
                               .flatMap(opt -> opt.fold(() -> Promise.success(Option.none()),
                                                        this::deserializeValue));
        }

        @Override
        public Promise<Unit> put(K key, V value) {
            return put(key, value, config.defaultTtl());
        }

        @Override
        public Promise<Unit> put(K key, V value, Duration ttl) {
            return put(key, value, Option.some(ttl));
        }

        private Promise<Unit> put(K key, V value, Option<Duration> ttl) {
            return serializeKey(key)
                               .flatMap(keyBytes -> serializeValue(value)
                                                                  .map(valueBytes -> {
                                                                           storage.put(keyBytes, valueBytes, ttl);
                                                                           return Unit.unit();
                                                                       }));
        }

        @Override
        public Promise<Unit> remove(K key) {
            return serializeKey(key)
                               .map(keyBytes -> {
                                   storage.remove(keyBytes);
                                   return Unit.unit();
                               });
        }

        @Override
        public Promise<Unit> clear() {
            storage.clear();
            return Promise.success(Unit.unit());
        }

        @Override
        public String name() {
            return config.name();
        }

        private Promise<byte[]> serializeKey(K key) {
            return serializerFactory.serializer()
                                    .flatMap(serializer -> Promise.lift(e -> InfraSliceError.CacheError.cacheError("Failed to serialize key",
                                                                                                                   e),
                                                                        () -> serializer.encode(key)));
        }

        private Promise<byte[]> serializeValue(V value) {
            return serializerFactory.serializer()
                                    .flatMap(serializer -> Promise.lift(e -> InfraSliceError.CacheError.cacheError("Failed to serialize value",
                                                                                                                   e),
                                                                        () -> serializer.encode(value)));
        }

        @SuppressWarnings("unchecked")
        private Promise<Option<V>> deserializeValue(byte[] bytes) {
            return serializerFactory.deserializer()
                                    .flatMap(deserializer -> Promise.lift(e -> InfraSliceError.CacheError.cacheError("Failed to deserialize value",
                                                                                                                     e),
                                                                          () -> (V) deserializer.decode(bytes)))
                                    .map(Option::some);
        }
    }
}
