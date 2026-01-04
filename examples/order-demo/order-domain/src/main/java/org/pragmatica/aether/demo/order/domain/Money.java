package org.pragmatica.aether.demo.order.domain;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.math.BigDecimal;

public record Money(BigDecimal amount, Currency currency) {
    private static final Cause CURRENCY_MISMATCH = Causes.cause("Cannot operate on different currencies");

    public static Money usd(BigDecimal amount) {
        return new Money(amount, Currency.USD);
    }

    public static Money usd(String amount) {
        return new Money(new BigDecimal(amount), Currency.USD);
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
