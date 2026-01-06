package org.pragmatica.aether.example.order;

import org.pragmatica.aether.example.fulfillment.ShippingOption;
import org.pragmatica.aether.example.payment.PaymentMethod;
import org.pragmatica.aether.example.shared.Address;
import org.pragmatica.aether.example.shared.CustomerId;
import org.pragmatica.aether.example.shared.LineItem;
import org.pragmatica.aether.example.shared.OrderId;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.util.List;

/**
 * Validated order request - if this exists, all fields are valid.
 */
public record ValidOrder(OrderId orderId,
                         CustomerId customerId,
                         List<LineItem> items,
                         Address shippingAddress,
                         PaymentMethod paymentMethod,
                         ShippingOption shippingOption,
                         Option<String> discountCode) {
    /**
     * Parse and validate raw request into ValidOrder.
     */
    public static Result<ValidOrder> validOrder(PlaceOrderRequest raw) {
        return Result.all(CustomerId.customerId(raw.customerId()),
                          LineItem.lineItems(raw.items()),
                          validateAddress(raw.shippingAddress()),
                          validatePayment(raw.paymentMethod()))
                     .map((customerId, items, address, payment) -> new ValidOrder(OrderId.generate(),
                                                                                  customerId,
                                                                                  items,
                                                                                  address,
                                                                                  payment,
                                                                                  raw.shippingOption(),
                                                                                  raw.discountCode()));
    }

    private static Result<Address> validateAddress(PlaceOrderRequest.RawAddress raw) {
        return Address.address(raw.street(), raw.city(), raw.state(), raw.postalCode(), raw.country());
    }

    private static Result<PaymentMethod> validatePayment(PlaceOrderRequest.RawPaymentMethod raw) {
        return PaymentMethod.paymentMethod(raw.cardNumber(),
                                           raw.expiryMonth(),
                                           raw.expiryYear(),
                                           raw.cvv(),
                                           raw.cardholderName());
    }
}
