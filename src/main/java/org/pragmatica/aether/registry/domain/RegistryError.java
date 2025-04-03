package org.pragmatica.aether.registry.domain;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Functions.Fn3;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

public sealed interface RegistryError extends Causes.SimpleCause {
    record RegistryErrorCause(String message) implements RegistryError {
        @Override
        public String toString() {
            return completeMessage();
        }
    }

    static <T, U> Fn1<Result<T>, U> forValue(String template) {
        return (input) -> new RegistryErrorCause(String.format(template, input)).result();
    }

    static <T, U1, U2, U3> Fn3<Result<T>, U1, U2, U3> for3Values(String template) {
        return (input1, input2, input3) -> new RegistryErrorCause(String.format(template,
                                                                                input1,
                                                                                input2,
                                                                                input3)).result();
    }

}
