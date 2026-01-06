package org.pragmatica.aether.example.shared;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;
import org.pragmatica.lang.utils.Causes;

/**
 * Non-negative quantity value.
 */
public record Quantity(int value) {
    private static final Cause NON_POSITIVE = Causes.cause("Quantity must be positive");
    private static final Cause EXCEEDS_MAX = Causes.cause("Quantity exceeds maximum (10000)");

    public static final int MAX_QUANTITY = 10_000;

    public static Result<Quantity> quantity(int value) {
        return Verify.ensure(value, v -> v > 0, NON_POSITIVE)
                     .filter(EXCEEDS_MAX, v -> v <= MAX_QUANTITY)
                     .map(Quantity::new);
    }

    public Quantity add(Quantity other) {
        return new Quantity(Math.min(value + other.value, MAX_QUANTITY));
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(Math.max(0, value - other.value));
    }

    public boolean isGreaterThan(Quantity other) {
        return value > other.value;
    }

    public boolean isLessThanOrEqual(Quantity other) {
        return value <= other.value;
    }
}
