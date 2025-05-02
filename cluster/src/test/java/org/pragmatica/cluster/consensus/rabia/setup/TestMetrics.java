package org.pragmatica.cluster.consensus.rabia.setup;

import org.pragmatica.cluster.net.NodeId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// Metrics collection
public class TestMetrics {
    private final AtomicLong commandCount = new AtomicLong(0);
    private final Map<NodeId, List<Long>> latencies = new ConcurrentHashMap<>();
    private final Map<NodeId, Long> lastCommandSubmitTime = new ConcurrentHashMap<>();

    public void recordCommandSubmit(NodeId nodeId) {
        lastCommandSubmitTime.put(nodeId, System.nanoTime());
    }

    public void recordCommandCommit(NodeId nodeId) {
        Long submitTime = lastCommandSubmitTime.get(nodeId);
        if (submitTime != null) {
            long latency = System.nanoTime() - submitTime;
            latencies.computeIfAbsent(nodeId, _ -> new ArrayList<>()).add(latency);
            commandCount.incrementAndGet();
        }
    }

    public long getCommandCount() {
        return commandCount.get();
    }

    public double getOverallAverageLatency() {
        return latencies.values().stream()
                        .flatMap(Collection::stream)
                        .mapToLong(l -> l)
                        .average()
                        .orElse(0);
    }

    public void reset() {
        commandCount.set(0);
        latencies.clear();
        lastCommandSubmitTime.clear();
    }
}
