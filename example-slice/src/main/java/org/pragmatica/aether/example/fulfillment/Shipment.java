package org.pragmatica.aether.example.fulfillment;

import org.pragmatica.aether.example.shared.Address;
import org.pragmatica.aether.example.shared.OrderId;

import java.time.Instant;
import java.util.UUID;

/**
 * Created shipment with tracking information.
 */
public record Shipment(String shipmentId,
                       OrderId orderId,
                       String trackingNumber,
                       ShippingOption shippingOption,
                       Address destination,
                       ShipmentStatus status,
                       Instant estimatedDelivery,
                       Instant createdAt) {
    public enum ShipmentStatus {
        PENDING,
        LABEL_CREATED,
        PICKED_UP,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        EXCEPTION
    }

    public static Shipment create(OrderId orderId, Address destination, ShippingOption option) {
        return new Shipment("SHP-" + UUID.randomUUID()
                                        .toString()
                                        .substring(0, 8)
                                        .toUpperCase(),
                            orderId,
                            generateTrackingNumber(),
                            option,
                            destination,
                            ShipmentStatus.LABEL_CREATED,
                            Instant.now()
                                   .plus(option.estimatedDelivery()),
                            Instant.now());
    }

    private static String generateTrackingNumber() {
        return "1Z" + UUID.randomUUID()
                         .toString()
                         .replace("-", "")
                         .substring(0, 16)
                         .toUpperCase();
    }

    public Shipment updateStatus(ShipmentStatus newStatus) {
        return new Shipment(shipmentId,
                            orderId,
                            trackingNumber,
                            shippingOption,
                            destination,
                            newStatus,
                            estimatedDelivery,
                            createdAt);
    }

    public String trackingUrl() {
        return "https://track.example.com/" + trackingNumber;
    }
}
