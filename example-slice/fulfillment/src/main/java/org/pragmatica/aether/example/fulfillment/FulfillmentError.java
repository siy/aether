package org.pragmatica.aether.example.fulfillment;

import org.pragmatica.lang.Cause;

/**
 * Fulfillment-related errors.
 */
public sealed interface FulfillmentError extends Cause {
    record ShipmentNotFound(String trackingNumber) implements FulfillmentError {
        @Override
        public String message() {
            return "Shipment not found: " + trackingNumber;
        }
    }

    record SameDayNotAvailable(String state) implements FulfillmentError {
        @Override
        public String message() {
            return "Same-day delivery not available in " + state;
        }
    }

    record InvalidDestination(String reason) implements FulfillmentError {
        @Override
        public String message() {
            return "Invalid shipping destination: " + reason;
        }
    }

    static ShipmentNotFound shipmentNotFound(String trackingNumber) {
        return new ShipmentNotFound(trackingNumber);
    }

    static SameDayNotAvailable sameDayNotAvailable(String state) {
        return new SameDayNotAvailable(state);
    }
}
