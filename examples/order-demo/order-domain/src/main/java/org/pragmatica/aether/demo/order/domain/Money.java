package org.pragmatica.aether.demo.order.domain;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;
import org.pragmatica.lang.utils.Causes;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount, Currency currency) {
    public sealed interface MoneyError extends Cause {
        record NegativeAmount(BigDecimal amount) implements MoneyError {
            @Override
            public String message() {
                return "Amount cannot be negative: " + amount;
            }
        }
    }

    private static final Cause CURRENCY_MISMATCH = Causes.cause("Cannot operate on different currencies");
    private static final Fn1<Cause, BigDecimal> NEGATIVE_AMOUNT = MoneyError.NegativeAmount::new;

    public static Result<Money> usd(BigDecimal amount) {
        return Verify.ensure(amount,
                             a -> a.compareTo(BigDecimal.ZERO) >= 0,
                             NEGATIVE_AMOUNT)
                     .map(a -> a.setScale(2, RoundingMode.HALF_UP))
                     .map(a -> new Money(a, Currency.USD));
    }

    public static Result<Money> usd(String amount) {
        return Result.lift(_ -> new MoneyError.NegativeAmount(BigDecimal.ZERO),
                           () -> new BigDecimal(amount))
                     .flatMap(Money::usd);
    }

    public Result<Money> add(Money other) {
        if (!this.currency.equals(other.currency)) {
            return CURRENCY_MISMATCH.result();
        }
        return Result.success(new Money(this.amount.add(other.amount), this.currency));
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)),
                         this.currency);
    }

    public Result<Money> subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            return CURRENCY_MISMATCH.result();
        }
        return Result.success(new Money(this.amount.subtract(other.amount), this.currency));
    }
}
