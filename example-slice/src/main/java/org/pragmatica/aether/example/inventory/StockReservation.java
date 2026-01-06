package org.pragmatica.aether.example.inventory;

import org.pragmatica.aether.example.shared.OrderId;

import java.time.Instant;
import java.util.UUID;

/**
 * Confirmed stock reservation.
 */
public record StockReservation(String reservationId,
                               OrderId orderId,
                               Instant expiresAt) {
    private static final long RESERVATION_DURATION_MINUTES = 15;

    public static StockReservation create(OrderId orderId) {
        return new StockReservation(UUID.randomUUID()
                                        .toString(),
                                    orderId,
                                    Instant.now()
                                           .plusSeconds(RESERVATION_DURATION_MINUTES * 60));
    }

    public boolean isExpired() {
        return Instant.now()
                      .isAfter(expiresAt);
    }
}
