package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.utility.ULID;

public record SelectionVote(ULID messageId,
                            long timestamp,
                            String senderId,
                            ULID selectionId,
                            boolean accepted,
                            long viewNumber) implements RabiaMessage {
} 