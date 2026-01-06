package org.pragmatica.aether.example.order;

import org.pragmatica.aether.example.fulfillment.Shipment;
import org.pragmatica.aether.example.payment.PaymentResult;
import org.pragmatica.aether.example.pricing.PriceBreakdown;
import org.pragmatica.aether.example.shared.CustomerId;
import org.pragmatica.aether.example.shared.LineItem;
import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.aether.example.shared.OrderId;

import java.time.Instant;
import java.util.List;

/**
 * Complete order confirmation with all details.
 */
public record OrderConfirmation(OrderId orderId,
                                CustomerId customerId,
                                List<LineItem> items,
                                PriceBreakdown pricing,
                                PaymentResult payment,
                                Shipment shipment,
                                OrderStatus status,
                                Instant createdAt) {
    public enum OrderStatus {
        CONFIRMED,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }

    public static OrderConfirmation confirmed(ValidOrder order,
                                              PriceBreakdown pricing,
                                              PaymentResult payment,
                                              Shipment shipment) {
        return new OrderConfirmation(order.orderId(),
                                     order.customerId(),
                                     order.items(),
                                     pricing,
                                     payment,
                                     shipment,
                                     OrderStatus.CONFIRMED,
                                     Instant.now());
    }

    public Money total() {
        return pricing.total();
    }

    public String summary() {
        return String.format("Order %s confirmed. Total: %s. Tracking: %s. Estimated delivery: %s",
                             orderId.value(),
                             pricing.total(),
                             shipment.trackingNumber(),
                             shipment.estimatedDelivery());
    }
}
