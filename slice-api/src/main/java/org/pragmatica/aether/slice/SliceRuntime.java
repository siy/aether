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
 *     .flatMap(invoker -> invoker.invokeAndWait(targetSlice, method, request, ResponseType.class));
 * }</pre>
 * <p>
 * This approach allows slices to remain records (immutable) while still
 * accessing runtime services. The trade-off is global state, but this is
 * acceptable for ambient runtime services that are set once at startup.
 */
public final class SliceRuntime {
    private static volatile SliceInvokerFacade sliceInvoker;

    private SliceRuntime() {}

    /**
     * Get the SliceInvoker for inter-slice communication.
     * <p>
     * <b>Note:</b> This method throws an exception if the invoker is not configured.
     * Prefer using {@link #getSliceInvoker()} which returns a Result instead.
     *
     * @return the configured SliceInvoker
     * @throws IllegalStateException if SliceInvoker not configured
     * @deprecated Use {@link #getSliceInvoker()} for JBCT-compliant error handling
     */
    @Deprecated(since = "0.8.0", forRemoval = true)
    public static SliceInvokerFacade sliceInvoker() {
        var invoker = sliceInvoker;
        if (invoker == null) {
            throw new IllegalStateException(SliceRuntimeError.InvokerNotConfigured.INSTANCE.message());
        }
        return invoker;
    }

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
     * Clear all runtime services. Called during shutdown.
     */
    public static void clear() {
        sliceInvoker = null;
    }
}
