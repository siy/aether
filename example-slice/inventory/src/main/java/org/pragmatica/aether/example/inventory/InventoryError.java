package org.pragmatica.aether.example.inventory;

import org.pragmatica.aether.example.shared.ProductId;
import org.pragmatica.lang.Cause;

import java.util.List;

/**
 * Inventory-related errors.
 */
public sealed interface InventoryError extends Cause {
    record InsufficientStock(List<ProductId> products) implements InventoryError {
        @Override
        public String message() {
            var ids = products.stream()
                              .map(ProductId::value)
                              .toList();
            return "Insufficient stock for products: " + ids;
        }
    }

    record ReservationNotFound(String reservationId) implements InventoryError {
        @Override
        public String message() {
            return "Reservation not found: " + reservationId;
        }
    }

    record ReservationExpired(String reservationId) implements InventoryError {
        @Override
        public String message() {
            return "Reservation expired: " + reservationId;
        }
    }

    static InsufficientStock insufficientStock(List<ProductId> products) {
        return new InsufficientStock(List.copyOf(products));
    }

    static ReservationNotFound reservationNotFound(String id) {
        return new ReservationNotFound(id);
    }
}
