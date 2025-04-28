package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.consensus.Command;
import org.pragmatica.utility.ULID;

import java.util.List;

public record CommitMessage(ULID messageId,
                            long timestamp,
                            String senderId,
                            ULID batchId,
                            List<Command> commands) implements RabiaMessage {
}