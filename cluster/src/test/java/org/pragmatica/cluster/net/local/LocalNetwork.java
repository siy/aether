package org.pragmatica.cluster.net.local;

import org.pragmatica.cluster.consensus.ProtocolMessage;
import org.pragmatica.cluster.net.ViewChange;
import org.pragmatica.cluster.net.ClusterNetwork;
import org.pragmatica.cluster.net.NodeId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/// Local network implementation suitable for testing purposes
public class LocalNetwork<T extends ProtocolMessage> implements ClusterNetwork<T> {
    private final Map<NodeId, Consumer<T>> nodes = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public <M extends ProtocolMessage> void broadcast(M message) {
        nodes.values().forEach(consumer -> consumer.accept((T) message));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <M extends ProtocolMessage> void send(NodeId nodeId, M message) {
        nodes.get(nodeId).accept((T) message);
    }

    @Override
    public void connect(NodeId nodeId) {
    }

    @Override
    public void disconnect(NodeId nodeId) {
        nodes.remove(nodeId);
    }

    @Override
    public void observeViewChanges(Consumer<ViewChange> observer) {
    }

    @Override
    public void listen(Consumer<T> listener) {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    public void addNode(NodeId nodeId, Consumer<T> listener) {
        nodes.put(nodeId, listener);
    }
}
