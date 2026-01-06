package org.pragmatica.aether.example.pricing;

import org.pragmatica.aether.example.shared.Money;

import java.math.BigDecimal;

/**
 * Tax calculation result.
 */
public record TaxResult(Money taxAmount,
                        BigDecimal taxRate,
                        String jurisdiction) {
    public static TaxResult taxResult(Money amount, BigDecimal rate, String jurisdiction) {
        return new TaxResult(amount, rate, jurisdiction);
    }

    public String description() {
        return String.format("%s tax (%.1f%%): %s",
                             jurisdiction,
                             taxRate.multiply(BigDecimal.valueOf(100)),
                             taxAmount);
    }
}
