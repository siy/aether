package org.pragmatica.cluster.net;

import org.pragmatica.message.Message;

import java.util.List;

/// 
sealed public interface TopologyManagementMessage extends Message.Local {
    record AddNode(NodeInfo nodeInfo) implements TopologyManagementMessage {}

    record RemoveNode(NodeId nodeId) implements TopologyManagementMessage {}

    record DiscoverNodes(NodeId sender) implements TopologyManagementMessage {}

    record DiscoveredNodes(List<NodeInfo> nodeInfos) implements TopologyManagementMessage {}
}
