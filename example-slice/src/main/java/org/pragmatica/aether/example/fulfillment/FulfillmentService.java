package org.pragmatica.aether.example.fulfillment;

import org.pragmatica.aether.example.shared.Address;
import org.pragmatica.aether.example.shared.LineItem;
import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.lang.Promise;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fulfillment and shipping service.
 * <p>
 * Manages shipment creation, tracking, and shipping cost calculation.
 * In a real distributed system, this would be a separate @Slice module.
 */
public interface FulfillmentService {
    /**
     * Calculate shipping options and costs for items to a destination.
     */
    Promise<ShippingQuote> calculateShipping(CalculateShippingRequest request);

    /**
     * Create a shipment for an order.
     */
    Promise<Shipment> createShipment(CreateShipmentRequest request);

    /**
     * Track an existing shipment.
     */
    Promise<TrackingInfo> trackShipment(TrackShipmentRequest request);

    /**
     * Factory method.
     */
    static FulfillmentService fulfillmentService() {
        return new FulfillmentServiceImpl();
    }
}

class FulfillmentServiceImpl implements FulfillmentService {
    private final Map<String, Shipment> shipments = new ConcurrentHashMap<>();

    // States that don't support same-day delivery
    private static final Set<String> NO_SAME_DAY_STATES = Set.of("AK", "HI", "PR", "VI");

    // Free shipping threshold
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(100);

    @Override
    public Promise<ShippingQuote> calculateShipping(CalculateShippingRequest request) {
        var options = calculateAvailableOptions(request.items(), request.destination());
        return Promise.success(ShippingQuote.quote(options));
    }

    @Override
    public Promise<Shipment> createShipment(CreateShipmentRequest request) {
        // Validate shipping option is available for destination
        if (request.shippingOption() == ShippingOption.SAME_DAY && NO_SAME_DAY_STATES.contains(request.shippingAddress()
                                                                                                      .state()
                                                                                                      .toUpperCase())) {
            return FulfillmentError.sameDayNotAvailable(request.shippingAddress()
                                                               .state())
                                   .promise();
        }
        var shipment = Shipment.create(request.orderId(), request.shippingAddress(), request.shippingOption());
        shipments.put(shipment.trackingNumber(), shipment);
        return Promise.success(shipment);
    }

    @Override
    public Promise<TrackingInfo> trackShipment(TrackShipmentRequest request) {
        var shipment = shipments.get(request.trackingNumber());
        if (shipment == null) {
            return FulfillmentError.shipmentNotFound(request.trackingNumber())
                                   .promise();
        }
        return Promise.success(TrackingInfo.fromShipment(shipment));
    }

    private List<ShippingQuote.ShippingOptionQuote> calculateAvailableOptions(List<LineItem> items,
                                                                              Address destination) {
        var options = new ArrayList<ShippingQuote.ShippingOptionQuote>();
        var itemValue = calculateItemValue(items);
        var isFreeShippingEligible = itemValue.compareTo(FREE_SHIPPING_THRESHOLD) >= 0;
        for (var option : ShippingOption.values()) {
            // Skip same-day for remote states
            if (option == ShippingOption.SAME_DAY && NO_SAME_DAY_STATES.contains(destination.state()
                                                                                            .toUpperCase())) {
                continue;
            }
            var cost = calculateShippingCost(option, items, isFreeShippingEligible);
            var estimatedDelivery = Instant.now()
                                           .plus(option.estimatedDelivery());
            options.add(new ShippingQuote.ShippingOptionQuote(option, cost, estimatedDelivery));
        }
        return options;
    }

    private BigDecimal calculateItemValue(List<LineItem> items) {
        // Simplified - in real system would look up prices
        return items.stream()
                    .map(item -> BigDecimal.valueOf(item.quantity()
                                                        .value() * 50))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Money calculateShippingCost(ShippingOption option, List<LineItem> items, boolean freeShippingEligible) {
        // Free standard shipping for orders over threshold
        if (option == ShippingOption.STANDARD && freeShippingEligible) {
            return Money.ZERO_USD;
        }
        // Base cost from option
        var baseCost = option.cost();
        // Add weight-based surcharge for large orders
        var totalItems = items.stream()
                              .mapToInt(i -> i.quantity()
                                              .value())
                              .sum();
        if (totalItems > 10) {
            var surcharge = Money.usd(BigDecimal.valueOf(totalItems - 10)
                                                .multiply(BigDecimal.valueOf(0.5)))
                                 .unwrap();
            return baseCost.add(surcharge);
        }
        return baseCost;
    }
}
