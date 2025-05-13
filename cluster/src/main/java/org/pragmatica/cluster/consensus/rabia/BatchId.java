package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.utility.ULID;

/// Unique identifier for the proposal value (list of commands).
public record BatchId(String id) {
    public static BatchId batchId(String id) {
        return new BatchId(id);
    }

    public static BatchId randomBatchId() {
        return batchId(ULID.randomULID().encoded());
    }

    public static BatchId emptyBatchId() {
        return batchId("empty");
    }
}
