package org.pragmatica.aether.example.pricing;

import org.pragmatica.aether.example.shared.CustomerId;
import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.lang.Option;

/**
 * Request to apply a discount code.
 */
public record ApplyDiscountRequest(CustomerId customerId,
                                   Money subtotal,
                                   Option<String> discountCode) {
    public static ApplyDiscountRequest applyDiscountRequest(CustomerId customerId, Money subtotal, String code) {
        return new ApplyDiscountRequest(customerId,
                                        subtotal,
                                        Option.option(code)
                                              .filter(s -> !s.isBlank()));
    }

    public static ApplyDiscountRequest withoutCode(CustomerId customerId, Money subtotal) {
        return new ApplyDiscountRequest(customerId, subtotal, Option.empty());
    }
}
