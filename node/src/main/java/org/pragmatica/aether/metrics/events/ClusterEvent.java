package org.pragmatica.aether.metrics.events;
/**
 * Structured cluster events for observability and AI analysis.
 * <p>
 * Events are immutable and timestamped for correlation with metrics.
 */
public sealed interface ClusterEvent {
    long timestamp();

    // Node lifecycle events
    record NodeJoined(long timestamp, String nodeId, String address) implements ClusterEvent {}

    record NodeLeft(long timestamp, String nodeId, String reason) implements ClusterEvent {}

    record NodeSuspected(long timestamp, String nodeId) implements ClusterEvent {}

    // Slice lifecycle events
    record SliceDeployed(long timestamp, String artifact, String nodeId) implements ClusterEvent {}

    record SliceFailed(long timestamp, String artifact, String nodeId, String error) implements ClusterEvent {}

    record SliceUnloaded(long timestamp, String artifact, String nodeId) implements ClusterEvent {}

    record SliceScaled(long timestamp, String artifact, int oldCount, int newCount) implements ClusterEvent {}

    // Consensus events
    record LeaderChanged(long timestamp, String oldLeader, String newLeader) implements ClusterEvent {}

    record QuorumLost(long timestamp, int nodeCount, int quorumRequired) implements ClusterEvent {}

    record QuorumRestored(long timestamp, int nodeCount) implements ClusterEvent {}

    // Threshold and alert events
    record ThresholdBreached(long timestamp, String metric, double value, double threshold) implements ClusterEvent {}

    record ThresholdCleared(long timestamp, String metric, double value, double threshold) implements ClusterEvent {}

    // Rolling update events
    record RollingUpdateStarted(long timestamp, String artifact, String fromVersion, String toVersion) implements ClusterEvent {}

    record RollingUpdateCompleted(long timestamp, String artifact, String toVersion) implements ClusterEvent {}

    record RollingUpdateFailed(long timestamp, String artifact, String error) implements ClusterEvent {}

    // Factory methods for convenience
    static NodeJoined nodeJoined(String nodeId, String address) {
        return new NodeJoined(System.currentTimeMillis(), nodeId, address);
    }

    static NodeLeft nodeLeft(String nodeId, String reason) {
        return new NodeLeft(System.currentTimeMillis(), nodeId, reason);
    }

    static NodeSuspected nodeSuspected(String nodeId) {
        return new NodeSuspected(System.currentTimeMillis(), nodeId);
    }

    static SliceDeployed sliceDeployed(String artifact, String nodeId) {
        return new SliceDeployed(System.currentTimeMillis(), artifact, nodeId);
    }

    static SliceFailed sliceFailed(String artifact, String nodeId, String error) {
        return new SliceFailed(System.currentTimeMillis(), artifact, nodeId, error);
    }

    static SliceUnloaded sliceUnloaded(String artifact, String nodeId) {
        return new SliceUnloaded(System.currentTimeMillis(), artifact, nodeId);
    }

    static SliceScaled sliceScaled(String artifact, int oldCount, int newCount) {
        return new SliceScaled(System.currentTimeMillis(), artifact, oldCount, newCount);
    }

    static LeaderChanged leaderChanged(String oldLeader, String newLeader) {
        return new LeaderChanged(System.currentTimeMillis(), oldLeader, newLeader);
    }

    static QuorumLost quorumLost(int nodeCount, int quorumRequired) {
        return new QuorumLost(System.currentTimeMillis(), nodeCount, quorumRequired);
    }

    static QuorumRestored quorumRestored(int nodeCount) {
        return new QuorumRestored(System.currentTimeMillis(), nodeCount);
    }

    static ThresholdBreached thresholdBreached(String metric, double value, double threshold) {
        return new ThresholdBreached(System.currentTimeMillis(), metric, value, threshold);
    }

    static ThresholdCleared thresholdCleared(String metric, double value, double threshold) {
        return new ThresholdCleared(System.currentTimeMillis(), metric, value, threshold);
    }

    static RollingUpdateStarted rollingUpdateStarted(String artifact, String fromVersion, String toVersion) {
        return new RollingUpdateStarted(System.currentTimeMillis(), artifact, fromVersion, toVersion);
    }

    static RollingUpdateCompleted rollingUpdateCompleted(String artifact, String toVersion) {
        return new RollingUpdateCompleted(System.currentTimeMillis(), artifact, toVersion);
    }

    static RollingUpdateFailed rollingUpdateFailed(String artifact, String error) {
        return new RollingUpdateFailed(System.currentTimeMillis(), artifact, error);
    }
}
