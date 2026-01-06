package org.pragmatica.aether.example.payment;

import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.lang.Option;

/**
 * Request to refund a payment.
 */
public record RefundRequest(String transactionId,
                            Option<Money> partialAmount,
                            String reason) {
    public static RefundRequest fullRefund(String transactionId, String reason) {
        return new RefundRequest(transactionId, Option.empty(), reason);
    }

    public static RefundRequest partialRefund(String transactionId, Money amount, String reason) {
        return new RefundRequest(transactionId, Option.some(amount), reason);
    }
}
