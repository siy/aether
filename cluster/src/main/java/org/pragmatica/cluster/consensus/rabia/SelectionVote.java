package org.pragmatica.cluster.consensus.rabia;

import java.util.UUID;

public record SelectionVote(UUID messageId,
                          long timestamp,
                          String senderId,
                          UUID selectionId,
                          boolean accepted,
                          long viewNumber) implements RabiaMessage {
} 