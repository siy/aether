package org.pragmatica.aether.example.pricing;

import org.pragmatica.aether.example.shared.LineItem;
import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.aether.example.shared.ProductId;
import org.pragmatica.lang.Promise;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pricing service.
 * <p>
 * Calculates prices, applies discounts, and computes taxes.
 * In a real distributed system, this would be a separate @Slice module.
 */
public interface PricingService {
    /**
     * Calculate full price breakdown for items.
     */
    Promise<PriceBreakdown> calculatePrice(CalculatePriceRequest request);

    /**
     * Apply discount code and calculate discount amount.
     */
    Promise<DiscountResult> applyDiscount(ApplyDiscountRequest request);

    /**
     * Calculate tax based on shipping address.
     */
    Promise<TaxResult> calculateTax(CalculateTaxRequest request);

    /**
     * Factory method.
     */
    static PricingService pricingService() {
        return new PricingServiceImpl();
    }
}

class PricingServiceImpl implements PricingService {
    private final Map<ProductId, Money> productPrices = new ConcurrentHashMap<>();
    private final Map<String, DiscountCode> discountCodes = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> taxRates = new ConcurrentHashMap<>();
    private final Set<String> loyalCustomers = ConcurrentHashMap.newKeySet();

    PricingServiceImpl() {
        initializePrices();
        initializeDiscountCodes();
        initializeTaxRates();
    }

    @Override
    public Promise<PriceBreakdown> calculatePrice(CalculatePriceRequest request) {
        var linePrices = calculateLinePrices(request.items());
        var subtotal = linePrices.values()
                                 .stream()
                                 .map(PriceBreakdown.LinePrice::lineTotal)
                                 .reduce(Money.ZERO_USD, Money::add);
        return Promise.success(PriceBreakdown.builder()
                                             .linePrices(linePrices)
                                             .subtotal(subtotal)
                                             .build());
    }

    @Override
    public Promise<DiscountResult> applyDiscount(ApplyDiscountRequest request) {
        return request.discountCode()
                      .map(code -> applyCodeDiscount(code,
                                                     request.subtotal()))
                      .or(() -> applyAutomaticDiscount(request));
    }

    @Override
    public Promise<TaxResult> calculateTax(CalculateTaxRequest request) {
        var state = request.shippingAddress()
                           .state()
                           .toUpperCase();
        var rate = taxRates.getOrDefault(state, BigDecimal.valueOf(0.08));
        var taxAmount = request.subtotal()
                               .percentage((int)(rate.doubleValue() * 100));
        return Promise.success(TaxResult.taxResult(taxAmount, rate, state));
    }

    private Map<ProductId, PriceBreakdown.LinePrice> calculateLinePrices(List<LineItem> items) {
        var result = new HashMap<ProductId, PriceBreakdown.LinePrice>();
        for (var item : items) {
            var unitPrice = productPrices.getOrDefault(item.productId(), Money.ZERO_USD);
            var linePrice = PriceBreakdown.LinePrice.linePrice(item.productId(),
                                                               unitPrice,
                                                               item.quantity()
                                                                   .value());
            result.put(item.productId(), linePrice);
        }
        return result;
    }

    private Promise<DiscountResult> applyCodeDiscount(String code, Money subtotal) {
        var discount = discountCodes.get(code.toUpperCase());
        if (discount == null) {
            return PricingError.invalidDiscountCode(code)
                               .promise();
        }
        if (subtotal.amount()
                    .compareTo(discount.minimumPurchase()) < 0) {
            return PricingError.minimumNotMet(discount.minimumPurchase())
                               .promise();
        }
        var discountAmount = subtotal.percentage(discount.percentOff());
        return Promise.success(DiscountResult.percentOff(discount.percentOff(), discountAmount, code));
    }

    private Promise<DiscountResult> applyAutomaticDiscount(ApplyDiscountRequest request) {
        // Bulk discount: 10% off for orders over $500
        if (request.subtotal()
                   .amount()
                   .compareTo(BigDecimal.valueOf(500)) >= 0) {
            var discount = request.subtotal()
                                  .percentage(10);
            return Promise.success(DiscountResult.bulkDiscount(discount));
        }
        // Loyalty discount: 5% off for loyal customers
        if (loyalCustomers.contains(request.customerId()
                                           .value())) {
            var discount = request.subtotal()
                                  .percentage(5);
            return Promise.success(DiscountResult.loyaltyDiscount(discount));
        }
        return Promise.success(DiscountResult.noDiscount());
    }

    private void initializePrices() {
        addPrice("LAPTOP-PRO", "999.99");
        addPrice("MOUSE-WIRELESS", "49.99");
        addPrice("KEYBOARD-MECH", "149.99");
        addPrice("MONITOR-4K", "599.99");
        addPrice("HEADSET-BT", "79.99");
        addPrice("WEBCAM-HD", "89.99");
        addPrice("USB-HUB", "29.99");
        addPrice("CHARGER-65W", "39.99");
    }

    private void addPrice(String productId, String price) {
        ProductId.productId(productId)
                 .flatMap(id -> Money.usd(price)
                                     .map(p -> Map.entry(id, p)))
                 .onSuccess(entry -> productPrices.put(entry.getKey(),
                                                       entry.getValue()));
    }

    private void initializeDiscountCodes() {
        discountCodes.put("SAVE10", new DiscountCode("SAVE10", 10, BigDecimal.valueOf(50)));
        discountCodes.put("SAVE20", new DiscountCode("SAVE20", 20, BigDecimal.valueOf(100)));
        discountCodes.put("BLACKFRIDAY", new DiscountCode("BLACKFRIDAY", 30, BigDecimal.valueOf(200)));
        discountCodes.put("WELCOME", new DiscountCode("WELCOME", 15, BigDecimal.ZERO));
    }

    private void initializeTaxRates() {
        taxRates.put("CA", BigDecimal.valueOf(0.0725));
        // California
        taxRates.put("NY", BigDecimal.valueOf(0.08));
        // New York
        taxRates.put("TX", BigDecimal.valueOf(0.0625));
        // Texas
        taxRates.put("FL", BigDecimal.valueOf(0.06));
        // Florida
        taxRates.put("WA", BigDecimal.valueOf(0.065));
        // Washington
        taxRates.put("OR", BigDecimal.ZERO);
        // Oregon - no sales tax
        taxRates.put("DE", BigDecimal.ZERO);
    }

    private record DiscountCode(String code, int percentOff, BigDecimal minimumPurchase) {}
}
