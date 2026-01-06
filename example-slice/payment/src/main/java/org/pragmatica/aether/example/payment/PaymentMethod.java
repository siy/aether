package org.pragmatica.aether.example.payment;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;
import org.pragmatica.lang.utils.Causes;

/**
 * Payment method - credit card with basic validation.
 */
public record PaymentMethod(String cardNumber,
                            String expiryMonth,
                            String expiryYear,
                            String cvv,
                            String cardholderName) {
    private static final Fn1<Cause, String> INVALID_CARD = Causes.forOneValue("Invalid card number: %s");

    private static final Fn1<Cause, String> INVALID_EXPIRY = Causes.forOneValue("Invalid expiry: %s");

    private static final Fn1<Cause, String> INVALID_CVV = Causes.forOneValue("Invalid CVV: %s");

    public static Result<PaymentMethod> paymentMethod(String cardNumber,
                                                      String expiryMonth,
                                                      String expiryYear,
                                                      String cvv,
                                                      String cardholderName) {
        return Result.all(validateCardNumber(cardNumber),
                          validateExpiry(expiryMonth, expiryYear),
                          validateCvv(cvv),
                          Verify.ensure(cardholderName,
                                        Verify.Is::notBlank,
                                        Causes.forOneValue("Invalid cardholder name: {}")))
                     .map((card, _, validCvv, name) -> new PaymentMethod(card, expiryMonth, expiryYear, validCvv, name));
    }

    private static Result<String> validateCardNumber(String number) {
        return Verify.ensure(number, Verify.Is::notBlank, INVALID_CARD)
                     .map(n -> n.replaceAll("\\s+", ""))
                     .filter(INVALID_CARD, PaymentMethod::isValidCardNumber);
    }

    private static boolean isValidCardNumber(String number) {
        if (!number.matches("\\d{13,19}")) return false;
        return luhnCheck(number);
    }

    private static boolean luhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.digit(number.charAt(i), 10);
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private static Result<String> validateExpiry(String month, String year) {
        var combined = month + "/" + year;
        return Verify.ensure(month,
                             m -> m.matches("\\d{2}"),
                             _ -> INVALID_EXPIRY.apply(combined))
                     .flatMap(_ -> Verify.ensure(year,
                                                 y -> y.matches("\\d{2,4}"),
                                                 _ -> INVALID_EXPIRY.apply(combined)))
                     .map(_ -> combined);
    }

    private static Result<String> validateCvv(String cvv) {
        return Verify.ensure(cvv, c -> c.matches("\\d{3,4}"), INVALID_CVV);
    }

    public String maskedNumber() {
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    public String cardType() {
        return switch (cardNumber.charAt(0)) {
            case '4' -> "Visa";
            case '5' -> "Mastercard";
            case '3' -> "Amex";
            case '6' -> "Discover";
            default -> "Unknown";
        };
    }
}
