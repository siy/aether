package org.pragmatica.aether.example.pricing;

import org.pragmatica.aether.example.shared.Money;
import org.pragmatica.aether.example.shared.ProductId;

import java.util.List;
import java.util.Map;

/**
 * Detailed price breakdown for an order.
 */
public record PriceBreakdown(Map<ProductId, LinePrice> linePrices,
                             Money subtotal,
                             Money discountAmount,
                             Money taxAmount,
                             Money shippingCost,
                             Money total) {
    /**
     * Price for a single line item.
     */
    public record LinePrice(ProductId productId,
                            Money unitPrice,
                            int quantity,
                            Money lineTotal) {
        public static LinePrice linePrice(ProductId productId, Money unitPrice, int quantity) {
            return new LinePrice(productId,
                                 unitPrice,
                                 quantity,
                                 unitPrice.multiply(java.math.BigDecimal.valueOf(quantity)));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<ProductId, LinePrice> linePrices = Map.of();
        private Money subtotal = Money.ZERO_USD;
        private Money discountAmount = Money.ZERO_USD;
        private Money taxAmount = Money.ZERO_USD;
        private Money shippingCost = Money.ZERO_USD;

        public Builder linePrices(Map<ProductId, LinePrice> linePrices) {
            this.linePrices = Map.copyOf(linePrices);
            return this;
        }

        public Builder subtotal(Money subtotal) {
            this.subtotal = subtotal;
            return this;
        }

        public Builder discountAmount(Money discountAmount) {
            this.discountAmount = discountAmount;
            return this;
        }

        public Builder taxAmount(Money taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        public Builder shippingCost(Money shippingCost) {
            this.shippingCost = shippingCost;
            return this;
        }

        public PriceBreakdown build() {
            var total = subtotal.subtract(discountAmount)
                                .add(taxAmount)
                                .add(shippingCost);
            return new PriceBreakdown(linePrices, subtotal, discountAmount, taxAmount, shippingCost, total);
        }
    }

    public List<String> summary() {
        return List.of("Subtotal: " + subtotal,
                       "Discount: -" + discountAmount,
                       "Tax: " + taxAmount,
                       "Shipping: " + shippingCost,
                       "Total: " + total);
    }
}
