package org.pragmatica.cluster.consensus.rabia;

import java.util.UUID;

public record VoteMessage(UUID messageId,
                         long timestamp,
                         String senderId,
                         UUID batchId,
                         boolean accepted,
                         long viewNumber) implements RabiaMessage {
} 