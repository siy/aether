package org.pragmatica.aether.example.pricing;

import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.lang.Option;

/**
 * Result of discount calculation.
 */
public record DiscountResult(Money discountAmount,
                             Option<String> appliedCode,
                             String description) {
    public static DiscountResult noDiscount() {
        return new DiscountResult(Money.ZERO_USD, Option.empty(), "No discount applied");
    }

    public static DiscountResult percentOff(int percent, Money amount, String code) {
        return new DiscountResult(amount, Option.some(code), percent + "% off with code " + code);
    }

    public static DiscountResult loyaltyDiscount(Money amount) {
        return new DiscountResult(amount, Option.empty(), "Loyalty discount");
    }

    public static DiscountResult bulkDiscount(Money amount) {
        return new DiscountResult(amount, Option.empty(), "Bulk order discount");
    }

    public boolean hasDiscount() {
        return ! discountAmount.isZero();
    }
}
