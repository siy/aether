package org.pragmatica.aether.slice;
/**
 * Marker interface for aspect configuration types.
 * <p>
 * All aspect-specific configurations (cache, retry, timeout, etc.)
 * implement this interface to be used with {@link AspectFactory}.
 * <p>
 * Example implementations:
 * <ul>
 *   <li>{@code CacheConfig<K, V>} - cache configuration with key/value types</li>
 *   <li>{@code RetryConfig} - retry policy configuration</li>
 *   <li>{@code TimeoutConfig} - timeout configuration</li>
 * </ul>
 */
public interface AspectConfig {}
