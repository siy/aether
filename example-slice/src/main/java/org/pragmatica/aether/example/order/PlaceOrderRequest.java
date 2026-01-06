package org.pragmatica.aether.example.order;

import org.pragmatica.aether.example.fulfillment.ShippingOption;
import org.pragmatica.aether.example.shared.LineItem;
import org.pragmatica.lang.Option;

import java.util.List;

/**
 * Raw request to place an order - before validation.
 */
public record PlaceOrderRequest(String customerId,
                                List<LineItem.RawLineItem> items,
                                RawAddress shippingAddress,
                                RawPaymentMethod paymentMethod,
                                ShippingOption shippingOption,
                                Option<String> discountCode) {
    public record RawAddress(String street,
                             String city,
                             String state,
                             String postalCode,
                             String country) {}

    public record RawPaymentMethod(String cardNumber,
                                   String expiryMonth,
                                   String expiryYear,
                                   String cvv,
                                   String cardholderName) {}

    public static PlaceOrderRequest placeOrderRequest(String customerId,
                                                      List<LineItem.RawLineItem> items,
                                                      RawAddress address,
                                                      RawPaymentMethod payment,
                                                      ShippingOption shipping,
                                                      String discountCode) {
        return new PlaceOrderRequest(customerId,
                                     items,
                                     address,
                                     payment,
                                     shipping,
                                     Option.option(discountCode)
                                           .filter(s -> !s.isBlank()));
    }
}
