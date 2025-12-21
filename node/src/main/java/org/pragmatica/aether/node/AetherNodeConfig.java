package org.pragmatica.aether.node;

import org.pragmatica.aether.slice.SliceActionConfig;
import org.pragmatica.cluster.consensus.rabia.ProtocolConfig;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.net.NodeInfo;
import org.pragmatica.cluster.topology.ip.TopologyConfig;
import org.pragmatica.lang.io.TimeSpan;

import java.util.List;

import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Configuration for an Aether cluster node.
 *
 * @param topology     Cluster topology configuration
 * @param protocol     Consensus protocol configuration
 * @param sliceAction  Slice lifecycle configuration
 * @param managementPort Port for HTTP management API (0 to disable)
 */
public record AetherNodeConfig(
        TopologyConfig topology,
        ProtocolConfig protocol,
        SliceActionConfig sliceAction,
        int managementPort
) {
    public static final int DEFAULT_MANAGEMENT_PORT = 8080;
    public static final int MANAGEMENT_DISABLED = 0;
    public static AetherNodeConfig aetherNodeConfig(NodeId self,
                                                    int port,
                                                    List<NodeInfo> coreNodes) {
        return aetherNodeConfig(self, port, coreNodes, SliceActionConfig.defaultConfiguration(), DEFAULT_MANAGEMENT_PORT);
    }

    public static AetherNodeConfig aetherNodeConfig(NodeId self,
                                                    int port,
                                                    List<NodeInfo> coreNodes,
                                                    SliceActionConfig sliceActionConfig) {
        return aetherNodeConfig(self, port, coreNodes, sliceActionConfig, DEFAULT_MANAGEMENT_PORT);
    }

    public static AetherNodeConfig aetherNodeConfig(NodeId self,
                                                    int port,
                                                    List<NodeInfo> coreNodes,
                                                    SliceActionConfig sliceActionConfig,
                                                    int managementPort) {
        var topology = new TopologyConfig(
                self,
                timeSpan(5).seconds(),  // reconciliation interval
                timeSpan(1).seconds(),  // ping interval
                coreNodes
        );

        return new AetherNodeConfig(topology, ProtocolConfig.defaultConfig(), sliceActionConfig, managementPort);
    }

    public static AetherNodeConfig testConfig(NodeId self, int port, List<NodeInfo> coreNodes) {
        var topology = new TopologyConfig(
                self,
                timeSpan(500).millis(),
                timeSpan(100).millis(),
                coreNodes
        );

        return new AetherNodeConfig(topology, ProtocolConfig.testConfig(), SliceActionConfig.defaultConfiguration(), MANAGEMENT_DISABLED);
    }

    public NodeId self() {
        return topology.self();
    }
}
