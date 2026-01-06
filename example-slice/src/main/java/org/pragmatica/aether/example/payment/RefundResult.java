package org.pragmatica.aether.example.payment;

import org.pragmatica.aether.example.shared.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Refund processing result.
 */
public record RefundResult(String refundId,
                           String originalTransactionId,
                           Money refundedAmount,
                           Instant processedAt) {
    public static RefundResult refundResult(String originalTransactionId, Money amount) {
        return new RefundResult("REF-" + UUID.randomUUID()
                                            .toString()
                                            .substring(0, 8)
                                            .toUpperCase(),
                                originalTransactionId,
                                amount,
                                Instant.now());
    }
}
