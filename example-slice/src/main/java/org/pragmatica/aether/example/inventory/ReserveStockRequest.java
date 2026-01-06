package org.pragmatica.aether.example.inventory;

import org.pragmatica.aether.example.shared.LineItem;
import org.pragmatica.aether.example.shared.OrderId;

import java.util.List;

/**
 * Request to reserve stock for an order.
 */
public record ReserveStockRequest(OrderId orderId, List<LineItem> items) {
    public static ReserveStockRequest reserveStockRequest(OrderId orderId, List<LineItem> items) {
        return new ReserveStockRequest(orderId, List.copyOf(items));
    }
}
