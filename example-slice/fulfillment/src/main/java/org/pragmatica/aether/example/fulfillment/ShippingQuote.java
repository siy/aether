package org.pragmatica.aether.example.fulfillment;

import org.pragmatica.aether.example.shared.Money;

import java.time.Instant;
import java.util.List;

/**
 * Shipping cost quote with available options.
 */
public record ShippingQuote(List<ShippingOptionQuote> options,
                            Instant validUntil) {
    public record ShippingOptionQuote(ShippingOption option,
                                      Money cost,
                                      Instant estimatedDelivery) {}

    public static ShippingQuote quote(List<ShippingOptionQuote> options) {
        return new ShippingQuote(List.copyOf(options),
                                 Instant.now()
                                        .plusSeconds(3600));
    }
}
