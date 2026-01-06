package org.pragmatica.aether.example.fulfillment;

import org.pragmatica.aether.example.shared.Address;
import org.pragmatica.aether.example.shared.LineItem;

import java.util.List;

/**
 * Request to calculate shipping options and costs.
 */
public record CalculateShippingRequest(List<LineItem> items, Address destination) {
    public static CalculateShippingRequest calculateShippingRequest(List<LineItem> items, Address destination) {
        return new CalculateShippingRequest(List.copyOf(items), destination);
    }
}
