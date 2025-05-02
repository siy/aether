package org.pragmatica.cluster.consensus.rabia.setup;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

// Class to track notifications from state changes
public record TestStateChangeObserver(NodeId id, TestMetrics testMetrics) implements Consumer<Notification> {
    private static final Logger log = LoggerFactory.getLogger(TestStateChangeObserver.class);

    @Override
    public void accept(Notification notification) {
        log.debug("Node {} received state change: {}", id, notification);
        testMetrics.recordCommandCommit(id);
    }
}
