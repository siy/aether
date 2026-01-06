package org.pragmatica.aether.example.fulfillment;

import org.pragmatica.aether.example.shared.Money;

import java.time.Duration;

/**
 * Available shipping options.
 */
public enum ShippingOption {
    STANDARD("Standard Shipping", Duration.ofDays(5), Money.ZERO_USD),
    EXPRESS("Express Shipping", Duration.ofDays(2), createMoney("9.99")),
    OVERNIGHT("Overnight Shipping", Duration.ofDays(1), createMoney("24.99")),
    SAME_DAY("Same Day Delivery", Duration.ofHours(4), createMoney("49.99"));
    private final String displayName;
    private final Duration estimatedDelivery;
    private final Money cost;
    ShippingOption(String displayName, Duration estimatedDelivery, Money cost) {
        this.displayName = displayName;
        this.estimatedDelivery = estimatedDelivery;
        this.cost = cost;
    }
    public String displayName() {
        return displayName;
    }
    public Duration estimatedDelivery() {
        return estimatedDelivery;
    }
    public Money cost() {
        return cost;
    }
    private static Money createMoney(String amount) {
        return Money.usd(amount)
                    .unwrap();
    }
}
