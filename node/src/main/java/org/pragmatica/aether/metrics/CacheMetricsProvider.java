package org.pragmatica.aether.metrics;
/**
 * Provider interface for cache layer metrics.
 * <p>
 * Implementations can provide metrics from any cache implementation
 * (in-memory, Redis, etc.) for inclusion in comprehensive snapshots.
 */
public interface CacheMetricsProvider {
    /**
     * Get the total memory used by the cache in bytes.
     * <p>
     * For in-memory caches, this should estimate the actual memory footprint.
     * For external caches (Redis, etc.), this may return memory reported by the cache server.
     *
     * @return memory used in bytes, or 0 if not available
     */
    long memoryUsedBytes();

    /**
     * Get the cache hit rate as a ratio (0.0-1.0).
     * <p>
     * Calculated as: hits / (hits + misses)
     *
     * @return hit rate ratio, or 0.0 if no requests have been made
     */
    double hitRate();

    /**
     * Get the current number of entries in the cache.
     *
     * @return entry count
     */
    long entryCount();

    /**
     * Get the total number of cache hits.
     *
     * @return hit count
     */
    long hits();

    /**
     * Get the total number of cache misses.
     *
     * @return miss count
     */
    long misses();
}
