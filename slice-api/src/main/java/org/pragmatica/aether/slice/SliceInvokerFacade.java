package org.pragmatica.aether.slice;

import org.pragmatica.lang.Promise;

/**
 * Facade interface for slice invocation.
 * This is a simplified interface in slice-api that the full SliceInvoker implements.
 * <p>
 * Slices use this to call other slices without depending on the node module.
 */
@FunctionalInterface
public interface SliceInvokerFacade {
    /**
     * Invoke a method on another slice and wait for the response.
     *
     * @param sliceArtifact  Target slice artifact coordinate (e.g., "org.example:my-slice:1.0.0")
     * @param methodName     Name of the method to invoke
     * @param request        Request object
     * @param responseType   Expected response type
     * @param <R>            Response type
     * @return Promise resolving to the response
     */
    <R> Promise<R> invokeAndWait(String sliceArtifact,
                                 String methodName,
                                 Object request,
                                 Class<R> responseType);
}
