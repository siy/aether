package org.pragmatica.aether.slice;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

/**
 * A pre-parsed, reusable handle for invoking a specific method on a specific slice.
 * <p>
 * Created via {@link SliceInvokerFacade#methodHandle(String, String, Class, Class)}.
 * <p>
 * Using a MethodHandle avoids repeated parsing of artifact coordinates and method names,
 * significantly improving performance for repeated invocations.
 *
 * @param <Req>  Request type
 * @param <Resp> Response type
 */
public interface MethodHandle<Req, Resp> {
    /**
     * Invoke the method and wait for response.
     *
     * @param request Request object
     * @return Promise resolving to the response
     */
    Promise<Resp> invoke(Req request);

    /**
     * Invoke the method without waiting for response (fire-and-forget).
     *
     * @param request Request object
     * @return Promise resolving when request is sent
     */
    Promise<Unit> fireAndForget(Req request);

    /**
     * Get the target slice artifact coordinate (e.g., "org.example:my-slice:1.0.0").
     * Useful for debugging and logging.
     *
     * @return Artifact coordinate string
     */
    String artifactCoordinate();

    /**
     * Get the target method name.
     * Useful for debugging and logging.
     *
     * @return Method name
     */
    MethodName methodName();
}
