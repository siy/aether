package org.pragmatica.aether.infra.aspect;

import org.pragmatica.aether.slice.serialization.SerializerFactory;

/**
 * Factory for creating typed {@link Cache} instances.
 * <p>
 * By swapping factory implementations, you can switch between
 * local (in-memory), off-heap, or distributed cache backends.
 */
public interface CacheFactory {
    /**
     * Create a typed cache with the given configuration.
     *
     * @param config Cache configuration including name, types, TTL, limits
     * @param <K>    Key type
     * @param <V>    Value type
     * @return Cache instance
     */
    <K, V> Cache<K, V> create(CacheConfig<K, V> config);

    /**
     * Create a CacheFactory backed by the given storage.
     *
     * @param storage           Local storage backend
     * @param serializerFactory Serializer for keys and values
     * @return CacheFactory instance
     */
    static CacheFactory cacheFactory(CacheStorage storage, SerializerFactory serializerFactory) {
        return new DefaultCacheFactory(storage, serializerFactory);
    }
}
