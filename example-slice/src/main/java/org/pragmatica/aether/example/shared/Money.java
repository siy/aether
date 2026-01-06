package org.pragmatica.aether.example.shared;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;
import org.pragmatica.lang.utils.Causes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Monetary value with currency.
 * Immutable, rounded to 2 decimal places.
 */
public record Money(BigDecimal amount, Currency currency) {
    private static final Cause NEGATIVE_AMOUNT = Causes.cause("Amount cannot be negative");

    private static final Fn1<Cause, String> INVALID_CURRENCY = Causes.forOneValue("Invalid currency: %s");

    public static final Currency USD = Currency.getInstance("USD");
    public static final Currency EUR = Currency.getInstance("EUR");

    public static final Money ZERO_USD = new Money(BigDecimal.ZERO, USD);

    public static Result<Money> money(BigDecimal amount, Currency currency) {
        return Verify.ensure(amount,
                             a -> a.compareTo(BigDecimal.ZERO) >= 0,
                             NEGATIVE_AMOUNT)
                     .map(a -> a.setScale(2, RoundingMode.HALF_UP))
                     .map(a -> new Money(a, currency));
    }

    public static Result<Money> money(String amount, String currencyCode) {
        return Result.all(parseAmount(amount),
                          parseCurrency(currencyCode))
                     .flatMap(Money::money);
    }

    public static Result<Money> usd(BigDecimal amount) {
        return money(amount, USD);
    }

    public static Result<Money> usd(String amount) {
        return parseAmount(amount)
                          .flatMap(Money::usd);
    }

    public Money add(Money other) {
        verifySameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        verifySameCurrency(other);
        return new Money(amount.subtract(other.amount)
                               .max(BigDecimal.ZERO),
                         currency);
    }

    public Money multiply(BigDecimal factor) {
        return new Money(amount.multiply(factor)
                               .setScale(2, RoundingMode.HALF_UP),
                         currency);
    }

    public Money percentage(int percent) {
        return multiply(BigDecimal.valueOf(percent)
                                  .divide(BigDecimal.valueOf(100),
                                          4,
                                          RoundingMode.HALF_UP));
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money other) {
        verifySameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    private void verifySameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    private static Result<BigDecimal> parseAmount(String raw) {
        return Result.lift(_ -> Causes.cause("Invalid amount: " + raw), () -> new BigDecimal(raw));
    }

    private static Result<Currency> parseCurrency(String code) {
        return Result.lift(_ -> INVALID_CURRENCY.apply(code), () -> Currency.getInstance(code));
    }

    @Override
    public String toString() {
        return currency.getSymbol() + amount.toPlainString();
    }
}
