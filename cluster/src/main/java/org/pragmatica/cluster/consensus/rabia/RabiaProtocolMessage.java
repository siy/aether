package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.consensus.Command;
import org.pragmatica.cluster.consensus.ProtocolMessage;
import org.pragmatica.cluster.net.NodeId;

import java.util.List;

public sealed interface RabiaProtocolMessage extends ProtocolMessage {
    NodeId sender();

    record Propose(NodeId sender, int slot, List<Command> commands) implements RabiaProtocolMessage {}

    record Vote(NodeId sender, int slot, boolean match) implements RabiaProtocolMessage {}

    record Decide(NodeId sender, int slot, List<Command> commands) implements RabiaProtocolMessage {}

    record SnapshotRequest(NodeId sender) implements RabiaProtocolMessage {}

    record SnapshotResponse(NodeId sender, byte[] snapshot) implements RabiaProtocolMessage {}

}
