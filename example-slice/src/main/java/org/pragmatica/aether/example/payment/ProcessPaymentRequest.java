package org.pragmatica.aether.example.payment;

import org.pragmatica.aether.example.shared.CustomerId;
import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.aether.example.shared.OrderId;

/**
 * Request to process a payment.
 */
public record ProcessPaymentRequest(OrderId orderId,
                                    CustomerId customerId,
                                    Money amount,
                                    PaymentMethod paymentMethod) {
    public static ProcessPaymentRequest processPaymentRequest(OrderId orderId,
                                                              CustomerId customerId,
                                                              Money amount,
                                                              PaymentMethod paymentMethod) {
        return new ProcessPaymentRequest(orderId, customerId, amount, paymentMethod);
    }
}
