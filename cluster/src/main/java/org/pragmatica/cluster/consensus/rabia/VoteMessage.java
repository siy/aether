package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.utility.ULID;

public record VoteMessage(ULID messageId,
                          long timestamp,
                          String senderId,
                          ULID batchId,
                          boolean accepted,
                          long viewNumber) implements RabiaMessage {
}