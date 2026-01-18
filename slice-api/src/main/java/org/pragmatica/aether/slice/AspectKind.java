package org.pragmatica.aether.slice;
/**
 * Available aspect types for method decoration.
 * <p>
 * Used with {@link WithAspects} annotation to specify which cross-cutting
 * concerns should be applied to slice methods.
 */
public enum AspectKind {
    /**
     * Cache method results based on input key.
     * Requires Cache to be available.
     */
    CACHE,
    /**
     * Log method invocations and results.
     */
    LOG,
    /**
     * Collect metrics (latency, success/failure rates).
     */
    METRICS,
    /**
     * Retry failed operations with backoff.
     */
    RETRY,
    /**
     * Apply timeout to method execution.
     */
    TIMEOUT
}
