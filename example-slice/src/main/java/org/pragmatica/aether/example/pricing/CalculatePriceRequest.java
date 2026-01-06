package org.pragmatica.aether.example.pricing;

import org.pragmatica.aether.example.shared.CustomerId;
import org.pragmatica.aether.example.shared.LineItem;

import java.util.List;

/**
 * Request to calculate total price for items.
 */
public record CalculatePriceRequest(CustomerId customerId,
                                    List<LineItem> items) {
    public static CalculatePriceRequest calculatePriceRequest(CustomerId customerId, List<LineItem> items) {
        return new CalculatePriceRequest(customerId, List.copyOf(items));
    }
}
