package org.pragmatica.aether.example.pricing;

import org.pragmatica.aether.example.shared.Address;
import org.pragmatica.aether.example.shared.Money;

/**
 * Request to calculate tax based on shipping address.
 */
public record CalculateTaxRequest(Money subtotal, Address shippingAddress) {
    public static CalculateTaxRequest calculateTaxRequest(Money subtotal, Address address) {
        return new CalculateTaxRequest(subtotal, address);
    }
}
