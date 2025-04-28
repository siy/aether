package org.pragmatica.cluster.consensus.rabia;

import java.util.List;
import java.util.UUID;

public record CommitMessage(UUID messageId,
                          long timestamp,
                          String senderId,
                          UUID batchId,
                          List<byte[]> commands) implements RabiaMessage {
} 