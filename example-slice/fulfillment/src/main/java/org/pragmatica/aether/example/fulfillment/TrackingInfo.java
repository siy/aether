package org.pragmatica.aether.example.fulfillment;

import java.time.Instant;
import java.util.List;

/**
 * Shipment tracking information with history.
 */
public record TrackingInfo(String trackingNumber,
                           Shipment.ShipmentStatus currentStatus,
                           Instant estimatedDelivery,
                           List<TrackingEvent> events) {
    public record TrackingEvent(Instant timestamp,
                                String location,
                                String description,
                                Shipment.ShipmentStatus status) {}

    public static TrackingInfo fromShipment(Shipment shipment) {
        var events = List.of(new TrackingEvent(shipment.createdAt(),
                                               "Origin",
                                               "Shipment created",
                                               Shipment.ShipmentStatus.LABEL_CREATED));
        return new TrackingInfo(shipment.trackingNumber(), shipment.status(), shipment.estimatedDelivery(), events);
    }
}
