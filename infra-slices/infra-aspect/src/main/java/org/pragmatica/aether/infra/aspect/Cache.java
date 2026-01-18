package org.pragmatica.aether.infra.aspect;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.time.Duration;

/**
 * Typed cache interface for key-value storage.
 * <p>
 * Provides async operations for caching with optional TTL.
 * Implementations may be local (in-memory) or distributed (DHT-backed).
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public interface Cache<K, V> {
    /**
     * Retrieve a value by key.
     *
     * @param key Cache key
     * @return Promise containing Option with value if present
     */
    Promise<Option<V>> get(K key);

    /**
     * Store a value with default TTL.
     *
     * @param key   Cache key
     * @param value Value to store
     * @return Promise completing when stored
     */
    Promise<Unit> put(K key, V value);

    /**
     * Store a value with specific TTL.
     *
     * @param key   Cache key
     * @param value Value to store
     * @param ttl   Time-to-live for this entry
     * @return Promise completing when stored
     */
    Promise<Unit> put(K key, V value, Duration ttl);

    /**
     * Remove a value by key.
     *
     * @param key Cache key
     * @return Promise completing when removed
     */
    Promise<Unit> remove(K key);

    /**
     * Clear all entries from the cache.
     *
     * @return Promise completing when cleared
     */
    Promise<Unit> clear();

    /**
     * Get cache name for observability.
     *
     * @return Cache name
     */
    String name();
}
