package org.pragmatica.aether.aspect;

import org.pragmatica.lang.Cause;

/**
 * Errors related to aspect infrastructure creation and management.
 */
public sealed interface AspectError extends Cause {
    /**
     * Requested aspect infrastructure type is not supported.
     */
    record UnsupportedAspect(Class< ?> requestedType) implements AspectError {
        public static UnsupportedAspect unsupportedAspect(Class< ?> type) {
            return new UnsupportedAspect(type);
        }

        @Override
        public String message() {
            return "Unsupported aspect infrastructure type: " + requestedType.getName();
        }
    }
}
