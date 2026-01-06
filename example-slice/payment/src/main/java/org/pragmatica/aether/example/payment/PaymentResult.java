package org.pragmatica.aether.example.payment;

import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.aether.example.shared.OrderId;

import java.time.Instant;
import java.util.UUID;

/**
 * Successful payment result.
 */
public record PaymentResult(String transactionId,
                            OrderId orderId,
                            Money amount,
                            String cardType,
                            String maskedCard,
                            Instant processedAt,
                            PaymentStatus status) {
    public enum PaymentStatus {
        AUTHORIZED,
        CAPTURED,
        PENDING_CAPTURE
    }

    public static PaymentResult authorized(OrderId orderId, Money amount, PaymentMethod method) {
        return new PaymentResult("TXN-" + UUID.randomUUID()
                                             .toString()
                                             .substring(0, 8)
                                             .toUpperCase(),
                                 orderId,
                                 amount,
                                 method.cardType(),
                                 method.maskedNumber(),
                                 Instant.now(),
                                 PaymentStatus.AUTHORIZED);
    }

    public PaymentResult capture() {
        return new PaymentResult(transactionId,
                                 orderId,
                                 amount,
                                 cardType,
                                 maskedCard,
                                 Instant.now(),
                                 PaymentStatus.CAPTURED);
    }
}
