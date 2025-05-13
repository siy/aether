package org.pragmatica.cluster.net;

import org.pragmatica.utility.ULID;

public sealed interface IdGenerator {

    static String generate() {
        return ULID.randomULID().encoded();
    }

    @SuppressWarnings("unused")
    record unused() implements IdGenerator {}
}
