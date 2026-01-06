package org.pragmatica.aether.example.shared;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Verify;
import org.pragmatica.lang.utils.Causes;

import java.util.UUID;

/**
 * Unique identifier for a customer.
 * Validates format: non-blank UUID string.
 */
public record CustomerId(String value) {
    private static final Fn1<Cause, String> INVALID_CUSTOMER_ID = Causes.forOneValue("Invalid customer ID: %s");

    public static Result<CustomerId> customerId(String raw) {
        return Verify.ensure(raw, Verify.Is::notBlank, INVALID_CUSTOMER_ID)
                     .flatMap(CustomerId::parseUUID)
                     .map(CustomerId::new);
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID()
                                  .toString());
    }

    private static Result<String> parseUUID(String raw) {
        return Result.lift(_ -> INVALID_CUSTOMER_ID.apply(raw),
                           () -> {
                               UUID.fromString(raw);
                               return raw;
                           });
    }
}
