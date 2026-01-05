package org.pragmatica.aether.ttm.error;

import org.pragmatica.lang.Cause;

/**
 * Errors that can occur during TTM operations.
 */
public sealed interface TTMError extends Cause {
    /**
     * Model file not found or cannot be loaded.
     */
    record ModelLoadFailed(String path, String reason) implements TTMError {
        @Override
        public String message() {
            return "Failed to load TTM model from " + path + ": " + reason;
        }
    }

    /**
     * Inference failed.
     */
    record InferenceFailed(String reason) implements TTMError {
        @Override
        public String message() {
            return "TTM inference failed: " + reason;
        }
    }

    /**
     * Insufficient data for prediction.
     */
    record InsufficientData(int available, int required) implements TTMError {
        @Override
        public String message() {
            return "Insufficient data for TTM prediction: " + available + " minutes available, " + required
                   + " required";
        }
    }

    /**
     * Not the leader node.
     */
    record NotLeader() implements TTMError {
        public static final NotLeader INSTANCE = new NotLeader();

        @Override
        public String message() {
            return "TTM operations can only be performed by the leader node";
        }
    }

    /**
     * TTM is disabled.
     */
    record Disabled() implements TTMError {
        public static final Disabled INSTANCE = new Disabled();

        @Override
        public String message() {
            return "TTM is disabled in configuration";
        }
    }
}
