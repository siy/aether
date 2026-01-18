package org.pragmatica.aether.infra.aspect;

import org.pragmatica.aether.slice.AspectConfig;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;
import org.pragmatica.lang.type.TypeToken;

import java.time.Duration;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Verify.ensure;

/**
 * Configuration for typed cache aspect.
 * <p>
 * Used with {@link AspectFactory} to create {@link Cache} instances.
 * Type tokens enable proper serialization of keys and values.
 *
 * @param name       Cache name (for observability and management)
 * @param keyType    Type token for cache key type (for serialization)
 * @param valueType  Type token for cache value type (for serialization)
 * @param defaultTtl Default time-to-live for cache entries (empty for no expiry)
 * @param maxEntries Maximum number of entries (0 for unlimited)
 */
public record CacheConfig<K, V>(String name,
                                TypeToken<K> keyType,
                                TypeToken<V> valueType,
                                Option<Duration> defaultTtl,
                                int maxEntries) implements AspectConfig {
    /**
     * Create cache configuration with name and key/value types.
     *
     * @param name      Cache name for observability
     * @param keyType   Type token for cache keys
     * @param valueType Type token for cache values
     * @return Result containing configuration or error
     */
    public static <K, V> Result<CacheConfig<K, V>> cacheConfig(String name,
                                                               TypeToken<K> keyType,
                                                               TypeToken<V> valueType) {
        return ensure(name, Verify.Is::notBlank)
                     .flatMap(_ -> ensure(keyType, Verify.Is::notNull))
                     .flatMap(_ -> ensure(valueType, Verify.Is::notNull))
                     .map(_ -> new CacheConfig<>(name,
                                                 keyType,
                                                 valueType,
                                                 none(),
                                                 0));
    }

    /**
     * Create cache configuration with name, key/value types and default TTL.
     *
     * @param name       Cache name for observability
     * @param keyType    Type token for cache keys
     * @param valueType  Type token for cache values
     * @param defaultTtl Default time-to-live for entries
     * @return Result containing configuration or error
     */
    public static <K, V> Result<CacheConfig<K, V>> cacheConfig(String name,
                                                               TypeToken<K> keyType,
                                                               TypeToken<V> valueType,
                                                               Duration defaultTtl) {
        return ensure(name, Verify.Is::notBlank)
                     .flatMap(_ -> ensure(keyType, Verify.Is::notNull))
                     .flatMap(_ -> ensure(valueType, Verify.Is::notNull))
                     .flatMap(_ -> ensure(defaultTtl, Verify.Is::notNull))
                     .map(_ -> new CacheConfig<>(name,
                                                 keyType,
                                                 valueType,
                                                 Option.some(defaultTtl),
                                                 0));
    }

    /**
     * Create cache configuration with name, key/value types, default TTL and max entries.
     *
     * @param name       Cache name for observability
     * @param keyType    Type token for cache keys
     * @param valueType  Type token for cache values
     * @param defaultTtl Default time-to-live for entries
     * @param maxEntries Maximum number of cache entries
     * @return Result containing configuration or error
     */
    public static <K, V> Result<CacheConfig<K, V>> cacheConfig(String name,
                                                               TypeToken<K> keyType,
                                                               TypeToken<V> valueType,
                                                               Duration defaultTtl,
                                                               int maxEntries) {
        return ensure(name, Verify.Is::notBlank)
                     .flatMap(_ -> ensure(keyType, Verify.Is::notNull))
                     .flatMap(_ -> ensure(valueType, Verify.Is::notNull))
                     .flatMap(_ -> ensure(defaultTtl, Verify.Is::notNull))
                     .filter(NEGATIVE_MAX_ENTRIES, _ -> maxEntries >= 0)
                     .map(_ -> new CacheConfig<>(name,
                                                 keyType,
                                                 valueType,
                                                 Option.some(defaultTtl),
                                                 maxEntries));
    }

    private static final org.pragmatica.lang.Cause NEGATIVE_MAX_ENTRIES = org.pragmatica.lang.utils.Causes.cause("maxEntries must not be negative");

    /**
     * Create a copy with a different max entries limit.
     */
    public CacheConfig<K, V> withMaxEntries(int max) {
        return new CacheConfig<>(name, keyType, valueType, defaultTtl, max);
    }

    /**
     * Create a copy with a different default TTL.
     */
    public CacheConfig<K, V> withDefaultTtl(Duration ttl) {
        return new CacheConfig<>(name, keyType, valueType, Option.some(ttl), maxEntries);
    }
}
