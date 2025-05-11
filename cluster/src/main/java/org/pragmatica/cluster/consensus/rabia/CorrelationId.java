package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.utility.ULID;

public interface CorrelationId {
    String id();

    static CorrelationId correlationId(String id) {
        record correlationId(String id) implements CorrelationId {}

        return new correlationId(id);
    }

    static CorrelationId randomCorrelationId() {
        return correlationId(ULID.randomULID().encoded());
    }

    static CorrelationId emptyCorrelationId() {
        return correlationId("");
    }
}
