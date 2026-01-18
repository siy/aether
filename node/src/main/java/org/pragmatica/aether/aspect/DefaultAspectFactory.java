package org.pragmatica.aether.aspect;

import org.pragmatica.aether.infra.aspect.Cache;
import org.pragmatica.aether.infra.aspect.CacheConfig;
import org.pragmatica.aether.infra.aspect.CacheFactory;
import org.pragmatica.aether.slice.AspectConfig;
import org.pragmatica.aether.slice.AspectFactory;
import org.pragmatica.lang.Result;

/**
 * Default implementation of {@link AspectFactory}.
 * Creates aspect infrastructure objects based on configuration type.
 */
public final class DefaultAspectFactory implements AspectFactory {
    private final CacheFactory cacheFactory;

    private DefaultAspectFactory(CacheFactory cacheFactory) {
        this.cacheFactory = cacheFactory;
    }

    public static DefaultAspectFactory defaultAspectFactory(CacheFactory cacheFactory) {
        return new DefaultAspectFactory(cacheFactory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends AspectConfig, R> Result<R> create(Class<R> infrastructureType, C config) {
        if (infrastructureType == Cache.class && config instanceof CacheConfig< ?, ?> cacheConfig) {
            return Result.success((R) cacheFactory.create((CacheConfig<Object, Object>) cacheConfig));
        }
        return AspectError.UnsupportedAspect.unsupportedAspect(infrastructureType)
                          .result();
    }
}
