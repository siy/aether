package org.pragmatica.aether.slice;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

/**
 * Provides access to runtime services for slices.
 * <p>
 * This is a static holder for runtime services that slices may need.
 * The runtime sets these services before activating slices.
 * <p>
 * Usage in slice:
 * <pre>{@code
 * SliceRuntime.getSliceInvoker()
 *     .async()
 *     .flatMap(invoker -> invoker.invoke(targetSlice, method, request, ResponseType.class));
 *
 * // For aspects (in generated code):
 * var cache = SliceRuntime.getAspectFactory()
 *     .flatMap(factory -> factory.create(Cache.class, cacheConfig))
 *     .unwrap();
 * var cachedFn = Aspects.withCaching(fn, Request::userId, cache);
 * }</pre>
 * <p>
 * This approach allows slices to remain records (immutable) while still
 * accessing runtime services. The trade-off is global state, but this is
 * acceptable for ambient runtime services that are set once at startup.
 */
public final class SliceRuntime {
    private static volatile SliceInvokerFacade sliceInvoker;
    private static volatile AspectFactory aspectFactory;

    private SliceRuntime() {}

    /**
     * Get the SliceInvoker for inter-slice communication.
     *
     * @return Result containing the SliceInvoker, or failure if not configured
     */
    public static Result<SliceInvokerFacade> getSliceInvoker() {
        return Option.option(sliceInvoker)
                     .toResult(SliceRuntimeError.InvokerNotConfigured.INSTANCE);
    }

    /**
     * Get the SliceInvoker if configured.
     *
     * @return Option containing the SliceInvoker, or empty if not configured
     */
    public static Option<SliceInvokerFacade> trySliceInvoker() {
        return Option.option(sliceInvoker);
    }

    /**
     * Configure the SliceInvoker. Called by the runtime during startup.
     *
     * @param invoker the SliceInvoker to use
     */
    public static void setSliceInvoker(SliceInvokerFacade invoker) {
        sliceInvoker = invoker;
    }

    /**
     * Get the AspectFactory for creating aspect infrastructure.
     *
     * @return Result containing the AspectFactory, or failure if not configured
     */
    public static Result<AspectFactory> getAspectFactory() {
        return Option.option(aspectFactory)
                     .toResult(SliceRuntimeError.AspectFactoryNotConfigured.INSTANCE);
    }

    /**
     * Get the AspectFactory if configured.
     *
     * @return Option containing the AspectFactory, or empty if not configured
     */
    public static Option<AspectFactory> tryAspectFactory() {
        return Option.option(aspectFactory);
    }

    /**
     * Configure the AspectFactory. Called by the runtime during startup.
     *
     * @param factory the AspectFactory to use
     */
    public static void setAspectFactory(AspectFactory factory) {
        aspectFactory = factory;
    }

    /**
     * Clear all runtime services. Called during shutdown.
     */
    public static void clear() {
        sliceInvoker = null;
        aspectFactory = null;
    }
}
