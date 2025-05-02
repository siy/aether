package org.pragmatica.cluster.consensus.rabia.setup;

import org.pragmatica.cluster.consensus.rabia.ProtocolConfig;
import org.pragmatica.cluster.consensus.rabia.RabiaEngine;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.net.local.LocalNetwork;
import org.pragmatica.cluster.state.kvstore.KVCommand;
import org.pragmatica.cluster.state.kvstore.KVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

// Test cluster implementation
public class TestCluster {
    private static final Logger log = LoggerFactory.getLogger(TestCluster.class);

    private final int size;
    private final LocalNetwork<RabiaProtocolMessage> network;
    private final List<NodeId> nodeIds = new ArrayList<>();
    private final Map<NodeId, RabiaEngine<RabiaProtocolMessage, KVCommand>> engines = new LinkedHashMap<>();
    private final Map<NodeId, KVStore<String, String>> stores = new LinkedHashMap<>();
    private final TestAddressBook addressBook;
    private final TestSerializer serializer = new TestSerializer();
    private final LocalNetwork.FaultInjector faultInjector;
    private final TestMetrics testMetrics = new TestMetrics();

    public TestCluster(int size) {
        this.size = size;
        this.addressBook = new TestAddressBook(size);
        this.faultInjector = new LocalNetwork.FaultInjector();
        this.network = new LocalNetwork<>(addressBook, faultInjector);
        this.network.start();

        // Create nodes
        for (int i = 1; i <= size; i++) {
            var id = NodeId.create("node-" + i);
            nodeIds.add(id);
            addNewNode(id);
        }
    }

    public void addNewNode(NodeId id) {
        var store = new KVStore<String, String>(serializer);
        var engine = new RabiaEngine<>(id, addressBook, network, store, ProtocolConfig.testConfig());
        network.addNode(id, engine::processMessage);
        stores.put(id, store);
        engines.put(id, engine);

        store.observeStateChanges(new TestStateChangeObserver(id, testMetrics));
    }

    public void disconnectNode(NodeId id) {
        network.disconnect(id);
    }

    public void reconnectNode(NodeId id) {
        addNewNode(id);
    }

    public void crashNode(NodeId id) {
        faultInjector.setNodeFault(id, LocalNetwork.FaultType.NODE_CRASH, true);
    }

    public void recoverNode(NodeId id) {
        faultInjector.setNodeFault(id, LocalNetwork.FaultType.NODE_CRASH, false);
        reconnectNode(id);
    }

    public void injectFault(LocalNetwork.FaultType type, boolean active) {
        faultInjector.setFault(type, active);
    }

    public void setMessageLossRate(double rate) {
        faultInjector.setMessageLossRate(rate);
    }

    public void setMessageDelay(long delayMillis) {
        faultInjector.setMessageDelayMillis(delayMillis);
    }

    public void createNetworkPartition(List<NodeId> group1, List<NodeId> group2) {
        network.createPartition(group1, group2);
    }

    public void healNetworkPartitions() {
        network.healPartitions();
    }

    public void injectByzantineFault(NodeId nodeId) {
        faultInjector.setNodeFault(nodeId, LocalNetwork.FaultType.NODE_BYZANTINE, true);
    }

    public void healByzantineFault(NodeId nodeId) {
        faultInjector.setNodeFault(nodeId, LocalNetwork.FaultType.NODE_BYZANTINE, false);
    }

    public boolean isFaultyNode(NodeId nodeId, LocalNetwork.FaultType type) {
        return faultInjector.isFaultyNode(nodeId, type);
    }

    public Map<String, String> getStoreState(NodeId id) {
        return stores.get(id).snapshot();
    }

    public boolean submitCommand(NodeId id, KVCommand command) {
        testMetrics.recordCommandSubmit(id);
        return engines.get(id).trySubmitCommands(List.of(command));
    }

    public boolean submitCommands(NodeId id, List<KVCommand> commands) {
        testMetrics.recordCommandSubmit(id);
        return engines.get(id).trySubmitCommands(commands);
    }

    public int getClusterSize() {
        return size;
    }

    public int getQuorumSize() {
        return addressBook.quorumSize();
    }

    public List<NodeId> getNodeIds() {
        return new ArrayList<>(nodeIds);
    }

    public TestMetrics getMetrics() {
        return testMetrics;
    }

    public void shutdown() {
        network.stop();
    }

    public void resetMetrics() {
        testMetrics.reset();
    }

    public boolean verifyConsistentState() {
        if (stores.isEmpty()) {
            return true;
        }

        Map<String, String> referenceState = getStoreState(nodeIds.getFirst());

        return stores.entrySet().stream()
                     .allMatch(entry -> {
                         Map<String, String> storeState = getStoreState(entry.getKey());
                         if (!storeState.equals(referenceState)) {
                             log.error("Node {} has inconsistent state", entry.getKey());

                             var set = new HashSet<>(storeState.keySet());
                             var set2 = new HashSet<>(referenceState.keySet());
                             set.removeAll(referenceState.keySet());
                             set2.removeAll(storeState.keySet());

                             log.error("Keys not found in reference state: {} \n and vice versa: {}", set, set2);

                             return false;
                         }
                         return true;
                     });
    }

    // Calculate a state digest for verification
    public String calculateStateDigest(NodeId nodeId) {
        try {
            Map<String, String> state = getStoreState(nodeId);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Sort keys for deterministic digest
            List<String> sortedKeys = new ArrayList<>(state.keySet());
            Collections.sort(sortedKeys);

            for (String key : sortedKeys) {
                digest.update(key.getBytes());
                digest.update(state.get(key).getBytes());
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    // Verify identical state digests across all nodes
    public boolean verifyStateDigests() {
        if (nodeIds.isEmpty()) {
            return true;
        }

        String referenceDigest = calculateStateDigest(nodeIds.getFirst());
        return nodeIds.stream()
                      .allMatch(id -> calculateStateDigest(id).equals(referenceDigest));
    }

    public void awaitNode(NodeId clientNode) {
        var engine = engines.get(clientNode);

        engine.startPromise().await();
    }

    public void awaitCluster() {
        awaitNode(getNodeIds().getFirst());
    }
}
