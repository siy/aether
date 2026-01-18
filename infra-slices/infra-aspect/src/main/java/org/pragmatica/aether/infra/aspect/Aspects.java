package org.pragmatica.aether.infra.aspect;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

/**
 * Utility methods for composing aspects.
 * <p>
 * These methods are used by generated code to wrap slice methods
 * with cross-cutting concerns like caching, logging, and metrics.
 */
public final class Aspects {
    private Aspects() {}

    /**
     * Wrap a function with caching behavior.
     * <p>
     * On cache hit, returns cached value without invoking the function.
     * On cache miss, invokes function, caches result, and returns it.
     * Cache failures are non-fatal - if cache.put() fails, the computed result is still returned.
     *
     * @param fn           Original function to wrap
     * @param keyExtractor Extracts cache key from input
     * @param cache        Cache for storing results
     * @param <T>          Input type
     * @param <K>          Cache key type
     * @param <R>          Result type
     * @return Cached function
     */
    public static <T, K, R> Fn1<Promise<R>, T> withCaching(Fn1<Promise<R>, T> fn,
                                                           Fn1<K, T> keyExtractor,
                                                           Cache<K, R> cache) {
        return input -> {
            var key = keyExtractor.apply(input);
            return cache.get(key)
                        .flatMap(opt -> opt.fold(() -> fn.apply(input)
                                                         .flatMap(result -> cache.put(key, result)
                                                                                 .recover(_ -> Unit.unit())
                                                                                 .map(_ -> result)),
                                                 Promise::success));
        };
    }

    /**
     * Wrap a function with caching, using the entire input as the key.
     * <p>
     * Convenience method when the input type itself is the cache key.
     *
     * @param fn    Original function to wrap
     * @param cache Cache for storing results (key type = input type)
     * @param <T>   Input type (also cache key type)
     * @param <R>   Result type
     * @return Cached function
     */
    public static <T, R> Fn1<Promise<R>, T> withCaching(Fn1<Promise<R>, T> fn,
                                                        Cache<T, R> cache) {
        return withCaching(fn, input -> input, cache);
    }
}
