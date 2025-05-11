package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.utility.ULID;

/// Unique identifier for the proposal value (list of commands).
public interface BatchId {
    String id();

    static BatchId batchId(String id) {
        record batchId(String id) implements BatchId {}

        return new batchId(id);
    }

    static BatchId randomBatchId() {
        return batchId(ULID.randomULID().encoded());
    }

    static BatchId emptyBatchId() {
        return batchId("empty");
    }
}
