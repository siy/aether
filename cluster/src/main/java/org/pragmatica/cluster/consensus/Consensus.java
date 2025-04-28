package org.pragmatica.cluster.consensus;

import java.util.List;

public interface Consensus<T extends ProtocolMessage> {
    void processMessage(T message);

    void submitCommands(List<Command> commands);
}
