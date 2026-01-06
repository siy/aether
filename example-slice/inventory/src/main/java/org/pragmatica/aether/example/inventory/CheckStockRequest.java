package org.pragmatica.aether.example.inventory;

import org.pragmatica.aether.example.shared.LineItem;

import java.util.List;

/**
 * Request to check stock availability for multiple items.
 */
public record CheckStockRequest(List<LineItem> items) {
    public static CheckStockRequest checkStockRequest(List<LineItem> items) {
        return new CheckStockRequest(List.copyOf(items));
    }
}
