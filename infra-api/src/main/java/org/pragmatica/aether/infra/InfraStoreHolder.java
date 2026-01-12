package org.pragmatica.aether.infra;

import org.pragmatica.lang.Option;

/**
 * Thread-safe holder for the global InfraStore instance.
 */
final class InfraStoreHolder {
    private static volatile InfraStore instance;

    private InfraStoreHolder() {}

    static Option<InfraStore> instance() {
        return Option.option(instance);
    }

    static void setInstance(InfraStore store) {
        instance = store;
    }

    static void clear() {
        instance = null;
    }
}
