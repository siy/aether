package org.pragmatica.aether.example.fulfillment;
/**
 * Request to track a shipment.
 */
public record TrackShipmentRequest(String trackingNumber) {
    public static TrackShipmentRequest trackShipmentRequest(String trackingNumber) {
        return new TrackShipmentRequest(trackingNumber);
    }
}
