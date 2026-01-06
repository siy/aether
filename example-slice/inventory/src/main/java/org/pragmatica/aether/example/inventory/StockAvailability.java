package org.pragmatica.aether.example.inventory;

import org.pragmatica.aether.example.shared.ProductId;
import org.pragmatica.aether.example.shared.Quantity;

import java.util.List;
import java.util.Map;

/**
 * Result of stock availability check.
 */
public record StockAvailability(Map<ProductId, Quantity> availableStock,
                                List<ProductId> unavailableItems) {
    public static StockAvailability fullyAvailable(Map<ProductId, Quantity> stock) {
        return new StockAvailability(Map.copyOf(stock), List.of());
    }

    public static StockAvailability partiallyAvailable(Map<ProductId, Quantity> stock, List<ProductId> unavailable) {
        return new StockAvailability(Map.copyOf(stock), List.copyOf(unavailable));
    }

    public boolean isFullyAvailable() {
        return unavailableItems.isEmpty();
    }

    public boolean hasUnavailableItems() {
        return ! unavailableItems.isEmpty();
    }
}
