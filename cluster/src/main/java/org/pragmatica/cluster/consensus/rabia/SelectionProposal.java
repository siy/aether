package org.pragmatica.cluster.consensus.rabia;

import java.util.List;
import java.util.UUID;

public record SelectionProposal(UUID messageId,
                              long timestamp,
                              String senderId,
                              UUID selectionId,
                              List<byte[]> commands,
                              long selectionTimestamp) implements RabiaMessage {
} 