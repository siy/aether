package org.pragmatica.aether.example.fulfillment;

import org.pragmatica.aether.example.shared.Address;
import org.pragmatica.aether.example.shared.LineItem;
import org.pragmatica.aether.example.shared.OrderId;

import java.util.List;

/**
 * Request to create a shipment for an order.
 */
public record CreateShipmentRequest(OrderId orderId,
                                    List<LineItem> items,
                                    Address shippingAddress,
                                    ShippingOption shippingOption) {
    public static CreateShipmentRequest createShipmentRequest(OrderId orderId,
                                                              List<LineItem> items,
                                                              Address address,
                                                              ShippingOption option) {
        return new CreateShipmentRequest(orderId, List.copyOf(items), address, option);
    }
}
