package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.consensus.Command;
import org.pragmatica.utility.ULID;

import java.util.List;

public record SelectionProposal(ULID messageId,
                                long timestamp,
                                String senderId,
                                ULID selectionId,
                                List<Command> commands,
                                long selectionTimestamp) implements RabiaMessage {
}