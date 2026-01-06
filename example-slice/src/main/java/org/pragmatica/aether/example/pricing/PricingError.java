package org.pragmatica.aether.example.pricing;

import org.pragmatica.lang.Cause;

import java.math.BigDecimal;

/**
 * Pricing-related errors.
 */
public sealed interface PricingError extends Cause {
    record InvalidDiscountCode(String code) implements PricingError {
        @Override
        public String message() {
            return "Invalid discount code: " + code;
        }
    }

    record MinimumPurchaseNotMet(BigDecimal minimum) implements PricingError {
        @Override
        public String message() {
            return "Minimum purchase of $" + minimum.toPlainString() + " required for this discount";
        }
    }

    record ProductNotFound(String productId) implements PricingError {
        @Override
        public String message() {
            return "Price not found for product: " + productId;
        }
    }

    static InvalidDiscountCode invalidDiscountCode(String code) {
        return new InvalidDiscountCode(code);
    }

    static MinimumPurchaseNotMet minimumNotMet(BigDecimal minimum) {
        return new MinimumPurchaseNotMet(minimum);
    }
}
