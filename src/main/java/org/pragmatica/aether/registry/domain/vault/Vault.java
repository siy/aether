package org.pragmatica.aether.registry.domain.vault;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public interface Vault {
    Id id();

    Name name();

    Loader loader();

    static Vault create(Id id, Name name, Loader loader) {
        record vault(Id id, Name name, Loader loader) implements Vault {}

        return new vault(id, name, loader);
    }

    interface Id {
        String id();

        static Result<Id> create(String id) {
            record id(String id) implements Id {}

            return ID_VALIDATOR.test(id)
                    ? Result.ok(new id(id))
                    : INVALID_VAULT_ID.apply(id).result();
        }
    }

    interface Name {
        String name();

        static Result<Name> create(String name) {
            record name(String name) implements Name {}

            return NAME_VALIDATOR.test(name)
                    ? Result.ok(new name(name))
                    : INVALID_VAULT_NAME.apply(name).result();
        }
    }

    Pattern ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_.-]+$");
    Predicate<String> NOT_NULL = Objects::nonNull;
    Predicate<String> ID_VALIDATOR = NOT_NULL.and(ID_PATTERN.asMatchPredicate());
    Predicate<String> NAME_VALIDATOR = NOT_NULL;

    Fn1<Cause, String> INVALID_VAULT_NAME = Causes.forValue("Invalid repository name: %s");
    Fn1<Cause, String> INVALID_VAULT_ID = Causes.forValue("Invalid repository ID: %s");
}
