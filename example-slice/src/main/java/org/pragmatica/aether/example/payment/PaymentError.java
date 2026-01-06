package org.pragmatica.aether.example.payment;

import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.lang.Cause;

/**
 * Payment-related errors.
 */
public sealed interface PaymentError extends Cause {
    record Declined(String reason) implements PaymentError {
        @Override
        public String message() {
            return "Payment declined: " + reason;
        }
    }

    record TransactionNotFound(String transactionId) implements PaymentError {
        @Override
        public String message() {
            return "Transaction not found: " + transactionId;
        }
    }

    record RefundExceedsOriginal(Money requested, Money original) implements PaymentError {
        @Override
        public String message() {
            return "Refund amount " + requested + " exceeds original payment " + original;
        }
    }

    record InvalidAmount(String reason) implements PaymentError {
        @Override
        public String message() {
            return "Invalid payment amount: " + reason;
        }
    }

    record FraudSuspected() implements PaymentError {
        @Override
        public String message() {
            return "Payment flagged for potential fraud - manual review required";
        }
    }

    record ProcessingFailed(Throwable cause) implements PaymentError {
        @Override
        public String message() {
            return "Payment processing failed: " + cause.getMessage();
        }
    }

    static Declined declined(String reason) {
        return new Declined(reason);
    }

    static TransactionNotFound transactionNotFound(String id) {
        return new TransactionNotFound(id);
    }

    static RefundExceedsOriginal refundExceedsOriginal(Money requested, Money original) {
        return new RefundExceedsOriginal(requested, original);
    }

    static InvalidAmount invalidAmount(String reason) {
        return new InvalidAmount(reason);
    }

    static FraudSuspected fraudSuspected() {
        return new FraudSuspected();
    }
}
