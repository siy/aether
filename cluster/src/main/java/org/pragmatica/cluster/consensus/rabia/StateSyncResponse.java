package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.utility.ULID;

public record StateSyncResponse(ULID messageId,
                                long timestamp,
                                String senderId,
                                long sequenceNumber,
                                byte[] stateSnapshot,
                                boolean isComplete) implements RabiaMessage {
}