package org.pragmatica.aether.infra.aspect;

import org.pragmatica.lang.Option;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Pluggable local storage interface for distributed cache.
 * <p>
 * This is the per-node storage backend, not the distributed cache itself.
 * Implementations must be thread-safe.
 */
public interface CacheStorage {
    /**
     * Retrieve a cached value by key.
     *
     * @param key Cache key (serialized)
     * @return Optional containing cached bytes if present and not expired
     */
    Option<byte[]> get(byte[] key);

    /**
     * Store a value in the cache.
     *
     * @param key   Cache key (serialized)
     * @param value Serialized value bytes
     * @param ttl   Optional time-to-live (empty uses storage default or no expiry)
     * @return Optional containing the old value if present, empty otherwise
     */
    Option<byte[]> put(byte[] key, byte[] value, Option<Duration> ttl);

    /**
     * Remove a specific entry from the cache.
     *
     * @param key Cache key (serialized) to remove
     * @return Optional containing the removed value if present, empty otherwise
     */
    Option<byte[]> remove(byte[] key);

    /**
     * Refresh the TTL for an existing entry.
     *
     * @param key    Cache key (serialized)
     * @param newTtl New time-to-live duration
     * @return true if the key existed and TTL was reset, false otherwise
     */
    boolean refresh(byte[] key, Duration newTtl);

    /**
     * Get a value and refresh its TTL atomically.
     * Uses the storage's default TTL for refresh.
     *
     * @param key Cache key (serialized)
     * @return Optional containing cached bytes if present and not expired
     */
    Option<byte[]> getAndRefresh(byte[] key);

    /**
     * Clear all entries from the cache.
     */
    void clear();

    /**
     * Get the current number of entries in the cache.
     *
     * @return Entry count
     */
    long size();

    /**
     * Get the number of cache hits since creation or last reset.
     *
     * @return Hit count
     */
    long hits();

    /**
     * Get the number of cache misses since creation or last reset.
     *
     * @return Miss count
     */
    long misses();

    /**
     * Estimate of memory used by cached data in bytes.
     *
     * @return Approximate memory usage in bytes
     */
    long memoryUsedBytes();

    /**
     * Reset hit/miss counters to zero.
     */
    void resetMetrics();

    /**
     * Invalidate all entries with keys matching the predicate.
     *
     * @param keyPredicate Predicate to test keys against
     * @return Number of entries invalidated
     */
    int invalidateMatching(Predicate<byte[]> keyPredicate);

    /**
     * Shutdown the storage and release resources.
     * Should be called when the cache is no longer needed.
     * Default implementation does nothing.
     */
    default void shutdown() {}

    /**
     * Set an eviction listener to be notified when entries are evicted.
     *
     * @param listener Listener to notify, or null to remove
     */
    default void setEvictionListener(EvictionListener listener) {}

    /**
     * Listener for cache eviction events.
     */
    @FunctionalInterface
    interface EvictionListener {
        /**
         * Called when an entry is evicted from the cache.
         *
         * @param key    The evicted key (serialized)
         * @param value  The evicted value (serialized)
         * @param reason The reason for eviction
         */
        void onEviction(byte[] key, byte[] value, EvictionReason reason);
    }

    /**
     * Reason for cache entry eviction.
     */
    enum EvictionReason {
        /** Entry expired due to TTL */
        EXPIRED,
        /** Entry evicted due to capacity limits */
        CAPACITY,
        /** Entry manually removed */
        MANUAL
    }
}
