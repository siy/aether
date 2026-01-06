package org.pragmatica.aether.example.order;

import org.pragmatica.aether.example.fulfillment.CalculateShippingRequest;
import org.pragmatica.aether.example.fulfillment.CreateShipmentRequest;
import org.pragmatica.aether.example.fulfillment.FulfillmentService;
import org.pragmatica.aether.example.fulfillment.Shipment;
import org.pragmatica.aether.example.fulfillment.ShippingQuote;
import org.pragmatica.aether.example.inventory.CheckStockRequest;
import org.pragmatica.aether.example.inventory.InventoryService;
import org.pragmatica.aether.example.inventory.ReleaseStockRequest;
import org.pragmatica.aether.example.inventory.ReserveStockRequest;
import org.pragmatica.aether.example.inventory.StockReservation;
import org.pragmatica.aether.example.payment.PaymentResult;
import org.pragmatica.aether.example.payment.PaymentService;
import org.pragmatica.aether.example.payment.ProcessPaymentRequest;
import org.pragmatica.aether.example.pricing.ApplyDiscountRequest;
import org.pragmatica.aether.example.pricing.CalculatePriceRequest;
import org.pragmatica.aether.example.pricing.CalculateTaxRequest;
import org.pragmatica.aether.example.pricing.DiscountResult;
import org.pragmatica.aether.example.pricing.PriceBreakdown;
import org.pragmatica.aether.example.pricing.PricingService;
import org.pragmatica.aether.example.pricing.TaxResult;
import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.aether.slice.annotation.Slice;
import org.pragmatica.lang.Promise;

/**
 * Place Order - Lean Slice orchestrating the complete order flow.
 * <p>
 * This demonstrates:
 * <ul>
 *   <li>Inter-slice dependencies (Inventory, Pricing, Payment, Fulfillment)</li>
 *   <li>Complex multi-step orchestration using sequencer pattern</li>
 *   <li>Fork-join for parallel operations (pricing + shipping quote)</li>
 *   <li>Error handling with compensation (release stock on payment failure)</li>
 *   <li>Parse-don't-validate pattern for request validation</li>
 * </ul>
 * <p>
 * Flow:
 * <pre>
 * 1. Validate request
 * 2. Check stock availability
 * 3. Calculate price + shipping (parallel)
 * 4. Apply discount
 * 5. Calculate tax
 * 6. Build final price
 * 7. Reserve stock
 * 8. Process payment (rollback stock if fails)
 * 9. Create shipment
 * 10. Return confirmation
 * </pre>
 */
@Slice
public interface PlaceOrder {
    /**
     * Place a complete order - validates, prices, pays, and ships.
     */
    Promise<OrderConfirmation> execute(PlaceOrderRequest request);

    /**
     * Factory method with all slice dependencies.
     * In production, these would be proxies calling remote slices.
     */
    static PlaceOrder placeOrder(InventoryService inventory,
                                 PricingService pricing,
                                 PaymentService payment,
                                 FulfillmentService fulfillment) {
        return new PlaceOrderImpl(inventory, pricing, payment, fulfillment);
    }
}

class PlaceOrderImpl implements PlaceOrder {
    private final InventoryService inventory;
    private final PricingService pricing;
    private final PaymentService payment;
    private final FulfillmentService fulfillment;

    PlaceOrderImpl(InventoryService inventory,
                   PricingService pricing,
                   PaymentService payment,
                   FulfillmentService fulfillment) {
        this.inventory = inventory;
        this.pricing = pricing;
        this.payment = payment;
        this.fulfillment = fulfillment;
    }

    @Override
    public Promise<OrderConfirmation> execute(PlaceOrderRequest request) {
        // Step 1: Validate request (parse-don't-validate)
        return ValidOrder.validOrder(request)
                         .async()
                         .flatMap(this::checkStockAvailability)
                         .flatMap(this::calculateFullPricing)
                         .flatMap(this::reserveStock)
                         .flatMap(this::processPayment)
                         .flatMap(this::createShipment)
                         .map(this::buildConfirmation);
    }

    // ========== Step Implementations ==========
    private Promise<ValidOrder> checkStockAvailability(ValidOrder order) {
        var checkRequest = CheckStockRequest.checkStockRequest(order.items());
        return inventory.checkStock(checkRequest)
                        .flatMap(availability -> availability.isFullyAvailable()
                                                 ? Promise.success(order)
                                                 : OrderError.outOfStock(availability.unavailableItems())
                                                             .promise());
    }

    private Promise<OrderWithPricing> calculateFullPricing(ValidOrder order) {
        // Fork: Calculate base price and shipping quote in parallel
        var pricePromise = pricing.calculatePrice(CalculatePriceRequest.calculatePriceRequest(order.customerId(),
                                                                                              order.items()));
        var shippingPromise = fulfillment.calculateShipping(CalculateShippingRequest.calculateShippingRequest(order.items(),
                                                                                                              order.shippingAddress()));
        return Promise.all(pricePromise, shippingPromise)
                      .flatMap((priceBreakdown, shippingQuote) -> applyDiscount(order, priceBreakdown, shippingQuote));
    }

    private Promise<OrderWithPricing> applyDiscount(ValidOrder order,
                                                    PriceBreakdown basePrice,
                                                    ShippingQuote shippingQuote) {
        var discountRequest = order.discountCode()
                                   .map(code -> ApplyDiscountRequest.applyDiscountRequest(order.customerId(),
                                                                                          basePrice.subtotal(),
                                                                                          code))
                                   .or(() -> ApplyDiscountRequest.withoutCode(order.customerId(),
                                                                              basePrice.subtotal()));
        return pricing.applyDiscount(discountRequest)
                      .flatMap(discount -> calculateTaxAndBuildPrice(order, basePrice, shippingQuote, discount));
    }

    private Promise<OrderWithPricing> calculateTaxAndBuildPrice(ValidOrder order,
                                                                PriceBreakdown basePrice,
                                                                ShippingQuote shippingQuote,
                                                                DiscountResult discount) {
        var subtotalAfterDiscount = basePrice.subtotal()
                                             .subtract(discount.discountAmount());
        var taxRequest = CalculateTaxRequest.calculateTaxRequest(subtotalAfterDiscount, order.shippingAddress());
        return pricing.calculateTax(taxRequest)
                      .map(tax -> buildFinalPrice(basePrice, shippingQuote, order, discount, tax))
                      .map(finalPrice -> new OrderWithPricing(order, finalPrice, shippingQuote));
    }

    private PriceBreakdown buildFinalPrice(PriceBreakdown basePrice,
                                           ShippingQuote shippingQuote,
                                           ValidOrder order,
                                           DiscountResult discount,
                                           TaxResult tax) {
        var shippingCost = findShippingCost(shippingQuote, order);
        return PriceBreakdown.builder()
                             .linePrices(basePrice.linePrices())
                             .subtotal(basePrice.subtotal())
                             .discountAmount(discount.discountAmount())
                             .taxAmount(tax.taxAmount())
                             .shippingCost(shippingCost)
                             .build();
    }

    private Money findShippingCost(ShippingQuote quote, ValidOrder order) {
        return quote.options()
                    .stream()
                    .filter(opt -> opt.option() == order.shippingOption())
                    .findFirst()
                    .map(ShippingQuote.ShippingOptionQuote::cost)
                    .orElse(Money.ZERO_USD);
    }

    private Promise<OrderWithReservation> reserveStock(OrderWithPricing context) {
        var reserveRequest = ReserveStockRequest.reserveStockRequest(context.order()
                                                                            .orderId(),
                                                                     context.order()
                                                                            .items());
        return inventory.reserveStock(reserveRequest)
                        .map(reservation -> new OrderWithReservation(context, reservation));
    }

    private Promise<OrderWithPayment> processPayment(OrderWithReservation context) {
        var paymentRequest = ProcessPaymentRequest.processPaymentRequest(context.context()
                                                                                .order()
                                                                                .orderId(),
                                                                         context.context()
                                                                                .order()
                                                                                .customerId(),
                                                                         context.context()
                                                                                .pricing()
                                                                                .total(),
                                                                         context.context()
                                                                                .order()
                                                                                .paymentMethod());
        return payment.processPayment(paymentRequest)
                      .map(result -> new OrderWithPayment(context, result))
                      .onFailure(cause -> releaseStockOnFailure(context));
    }

    private void releaseStockOnFailure(OrderWithReservation context) {
        var releaseRequest = ReleaseStockRequest.releaseStockRequest(context.reservation()
                                                                            .reservationId());
        // Fire-and-forget cleanup - in production, use proper compensation
        inventory.releaseStock(releaseRequest);
    }

    private Promise<OrderComplete> createShipment(OrderWithPayment context) {
        var shipmentRequest = CreateShipmentRequest.createShipmentRequest(context.reservation()
                                                                                 .context()
                                                                                 .order()
                                                                                 .orderId(),
                                                                          context.reservation()
                                                                                 .context()
                                                                                 .order()
                                                                                 .items(),
                                                                          context.reservation()
                                                                                 .context()
                                                                                 .order()
                                                                                 .shippingAddress(),
                                                                          context.reservation()
                                                                                 .context()
                                                                                 .order()
                                                                                 .shippingOption());
        return fulfillment.createShipment(shipmentRequest)
                          .map(shipment -> new OrderComplete(context, shipment));
    }

    private OrderConfirmation buildConfirmation(OrderComplete complete) {
        return OrderConfirmation.confirmed(complete.payment()
                                                   .reservation()
                                                   .context()
                                                   .order(),
                                           complete.payment()
                                                   .reservation()
                                                   .context()
                                                   .pricing(),
                                           complete.payment()
                                                   .payment(),
                                           complete.shipment());
    }

    // ========== Context Records for Pipeline ==========
    private record OrderWithPricing(ValidOrder order, PriceBreakdown pricing, ShippingQuote shippingQuote) {}

    private record OrderWithReservation(OrderWithPricing context, StockReservation reservation) {}

    private record OrderWithPayment(OrderWithReservation reservation, PaymentResult payment) {}

    private record OrderComplete(OrderWithPayment payment, Shipment shipment) {}
}
