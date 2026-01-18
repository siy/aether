package org.pragmatica.aether.slice;

import org.pragmatica.lang.Result;

/**
 * Factory for creating aspect infrastructure objects.
 * <p>
 * Creates infrastructure components (Cache, RetryPolicy, etc.) based on
 * configuration. Generated code then uses these with {@code Aspects.with*()}
 * methods to compose wrapped functions.
 * <p>
 * Example usage in generated code:
 * <pre>{@code
 * // Get factory from SliceRuntime
 * var factory = SliceRuntime.getAspectFactory().unwrap();
 *
 * // Create cache infrastructure
 * Cache<UserId, Profile> cache = factory.create(Cache.class, cacheConfig).unwrap();
 *
 * // Compose with key extractor
 * var cachedFn = Aspects.withCaching(originalFn, Request::userId, cache);
 * }</pre>
 */
public interface AspectFactory {
    /**
     * Create an aspect infrastructure object.
     *
     * @param infrastructureType The type of infrastructure to create (e.g., Cache.class)
     * @param config             Configuration for the infrastructure
     * @param <C>                Configuration type (must implement AspectConfig)
     * @param <R>                Result type (the infrastructure object)
     * @return Result containing the created infrastructure or error
     */
    <C extends AspectConfig, R> Result<R> create(Class<R> infrastructureType, C config);
}
