package org.pragmatica.cluster.net.ip;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.net.NodeInfo;
import org.pragmatica.lang.io.TimeSpan;

import java.util.List;

public record TopologyConfig(NodeId self, TimeSpan reconciliationInterval, TimeSpan pingInterval, List<NodeInfo> coreNodes) {
}
