package org.pragmatica.aether.example.order;

import org.pragmatica.aether.example.shared.ProductId;
import org.pragmatica.lang.Cause;

import java.util.List;

/**
 * Order processing errors.
 */
public sealed interface OrderError extends Cause {
    /**
     * Validation errors during request parsing.
     */
    record ValidationFailed(List<String> errors) implements OrderError {
        @Override
        public String message() {
            return "Order validation failed: " + String.join(", ", errors);
        }
    }

    /**
     * Requested items are out of stock.
     */
    record OutOfStock(List<ProductId> products) implements OrderError {
        @Override
        public String message() {
            var ids = products.stream()
                              .map(ProductId::value)
                              .toList();
            return "Items out of stock: " + ids;
        }
    }

    /**
     * Payment was declined.
     */
    record PaymentDeclined(String reason) implements OrderError {
        @Override
        public String message() {
            return "Payment declined: " + reason;
        }
    }

    /**
     * Order cannot be fulfilled to the specified address.
     */
    record FulfillmentFailed(String reason) implements OrderError {
        @Override
        public String message() {
            return "Cannot fulfill order: " + reason;
        }
    }

    /**
     * Internal error during order processing.
     */
    record ProcessingFailed(Throwable cause) implements OrderError {
        @Override
        public String message() {
            return "Order processing failed: " + cause.getMessage();
        }
    }

    static ValidationFailed validationFailed(List<String> errors) {
        return new ValidationFailed(errors);
    }

    static OutOfStock outOfStock(List<ProductId> products) {
        return new OutOfStock(products);
    }

    static PaymentDeclined paymentDeclined(String reason) {
        return new PaymentDeclined(reason);
    }

    static FulfillmentFailed fulfillmentFailed(String reason) {
        return new FulfillmentFailed(reason);
    }
}
