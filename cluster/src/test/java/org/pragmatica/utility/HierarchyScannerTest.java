package org.pragmatica.utility;

import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.cluster.topology.TopologyChangeNotification;
import org.pragmatica.cluster.topology.TopologyManagementMessage;
import org.pragmatica.cluster.state.kvstore.KVStateMachineNotification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HierarchyScannerTest {
    @Test
    void testHierarchy() {
        var classes = HierarchyScanner.walkUpTheTree(List.of(RabiaProtocolMessage.Synchronous.Propose.class,
                                                             TopologyChangeNotification.NodeAdded.class,
                                                             TopologyManagementMessage.AddNode.class,
                                                             KVStateMachineNotification.ValueGet.class,
                                                             QuorumStateNotification.class));

        assertEquals(16, classes.size());
    }
}