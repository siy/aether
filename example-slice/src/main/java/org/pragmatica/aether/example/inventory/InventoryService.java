package org.pragmatica.aether.example.inventory;

import org.pragmatica.aether.example.shared.LineItem;
import org.pragmatica.aether.example.shared.ProductId;
import org.pragmatica.aether.example.shared.Quantity;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inventory management service.
 * <p>
 * Manages product stock levels, availability checking, and reservations.
 * In a real distributed system, this would be a separate @Slice module.
 */
public interface InventoryService {
    /**
     * Check stock availability for requested items.
     */
    Promise<StockAvailability> checkStock(CheckStockRequest request);

    /**
     * Reserve stock for an order. Reservation expires after 15 minutes.
     */
    Promise<StockReservation> reserveStock(ReserveStockRequest request);

    /**
     * Release a stock reservation (e.g., on order cancellation).
     */
    Promise<Unit> releaseStock(ReleaseStockRequest request);

    /**
     * Factory method - creates inventory service with in-memory storage.
     */
    static InventoryService inventoryService() {
        return new InventoryServiceImpl();
    }
}

/**
 * In-memory implementation for demo purposes.
 */
class InventoryServiceImpl implements InventoryService {
    private final Map<ProductId, Quantity> stock = new ConcurrentHashMap<>();
    private final Map<String, ReserveStockRequest> reservations = new ConcurrentHashMap<>();

    InventoryServiceImpl() {
        initializeDemoStock();
    }

    @Override
    public Promise<StockAvailability> checkStock(CheckStockRequest request) {
        return Promise.success(calculateAvailability(request.items()));
    }

    @Override
    public Promise<StockReservation> reserveStock(ReserveStockRequest request) {
        var availability = calculateAvailability(request.items());
        if (availability.hasUnavailableItems()) {
            return InventoryError.insufficientStock(availability.unavailableItems())
                                 .promise();
        }
        var reservation = StockReservation.create(request.orderId());
        reservations.put(reservation.reservationId(), request);
        request.items()
               .forEach(this::decrementStock);
        return Promise.success(reservation);
    }

    @Override
    public Promise<Unit> releaseStock(ReleaseStockRequest request) {
        var reserved = reservations.remove(request.reservationId());
        if (reserved != null) {
            reserved.items()
                    .forEach(this::incrementStock);
        }
        return Promise.success(Unit.unit());
    }

    private StockAvailability calculateAvailability(List<LineItem> items) {
        var available = new HashMap<ProductId, Quantity>();
        var unavailable = new java.util.ArrayList<ProductId>();
        for (var item : items) {
            var currentStock = stock.getOrDefault(item.productId(), new Quantity(0));
            available.put(item.productId(), currentStock);
            if (currentStock.value() < item.quantity()
                                           .value()) {
                unavailable.add(item.productId());
            }
        }
        return unavailable.isEmpty()
               ? StockAvailability.fullyAvailable(available)
               : StockAvailability.partiallyAvailable(available, unavailable);
    }

    private void decrementStock(LineItem item) {
        stock.computeIfPresent(item.productId(),
                               (_, current) -> current.subtract(item.quantity()));
    }

    private void incrementStock(LineItem item) {
        stock.compute(item.productId(),
                      (_, current) -> current == null
                                      ? item.quantity()
                                      : current.add(item.quantity()));
    }

    private void initializeDemoStock() {
        ProductId.productId("LAPTOP-PRO")
                 .onSuccess(id -> stock.put(id,
                                            new Quantity(50)));
        ProductId.productId("MOUSE-WIRELESS")
                 .onSuccess(id -> stock.put(id,
                                            new Quantity(200)));
        ProductId.productId("KEYBOARD-MECH")
                 .onSuccess(id -> stock.put(id,
                                            new Quantity(100)));
        ProductId.productId("MONITOR-4K")
                 .onSuccess(id -> stock.put(id,
                                            new Quantity(30)));
        ProductId.productId("HEADSET-BT")
                 .onSuccess(id -> stock.put(id,
                                            new Quantity(75)));
        ProductId.productId("WEBCAM-HD")
                 .onSuccess(id -> stock.put(id,
                                            new Quantity(60)));
        ProductId.productId("USB-HUB")
                 .onSuccess(id -> stock.put(id,
                                            new Quantity(150)));
        ProductId.productId("CHARGER-65W")
                 .onSuccess(id -> stock.put(id,
                                            new Quantity(120)));
    }
}
