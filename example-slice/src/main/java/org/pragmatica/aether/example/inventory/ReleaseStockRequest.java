package org.pragmatica.aether.example.inventory;
/**
 * Request to release a stock reservation.
 */
public record ReleaseStockRequest(String reservationId) {
    public static ReleaseStockRequest releaseStockRequest(String reservationId) {
        return new ReleaseStockRequest(reservationId);
    }
}
