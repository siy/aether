package org.pragmatica.aether.agent.message;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a batch of telemetry data collected from one or more slices over a time window.
 * This message aggregates performance metrics, resource usage, and operational statistics
 * to reduce message volume while providing comprehensive monitoring data to the agent.
 * 
 * Telemetry batches are collected at regular intervals (typically 30 seconds) and contain
 * both raw metrics and pre-computed statistical summaries for efficient processing.
 */
public record SliceTelemetryBatch(
    String messageId,
    long timestamp,
    MessagePriority priority,
    String nodeId,
    List<SliceMetrics> sliceMetrics,
    Duration batchInterval,
    long totalEvents
) implements AgentMessage {
    
    /**
     * Creates a new telemetry batch with default priority and generated message ID.
     */
    public static SliceTelemetryBatch create(
        String nodeId,
        List<SliceMetrics> sliceMetrics,
        Duration batchInterval,
        long totalEvents
    ) {
        return new SliceTelemetryBatch(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            MessagePriority.LOW,  // Telemetry is typically background information
            nodeId,
            sliceMetrics,
            batchInterval,
            totalEvents
        );
    }
    
    /**
     * Metrics data for a single slice within the batch.
     * Contains both operational metrics and performance indicators.
     */
    public record SliceMetrics(
        String sliceId,
        String sliceName,
        PerformanceMetrics performance,
        ResourceMetrics resources,
        OperationalMetrics operations
    ) {
        
        /**
         * Performance-related metrics including latency, throughput, and error rates.
         */
        public record PerformanceMetrics(
            double averageLatencyMs,
            double p95LatencyMs,
            double p99LatencyMs,
            long requestCount,
            long errorCount,
            double errorRate,
            double throughputPerSecond
        ) {}
        
        /**
         * Resource utilization metrics including CPU, memory, and I/O usage.
         */
        public record ResourceMetrics(
            double cpuUtilizationPercent,
            long memoryUsedBytes,
            long memoryMaxBytes,
            double memoryUtilizationPercent,
            long networkBytesIn,
            long networkBytesOut,
            long diskBytesRead,
            long diskBytesWritten
        ) {}
        
        /**
         * Operational metrics including deployment status and health indicators.
         */
        public record OperationalMetrics(
            SliceStatus status,
            int replicaCount,
            int healthyReplicas,
            long uptime,
            int restartCount,
            Map<String, Object> customMetrics
        ) {}
        
        /**
         * Current operational status of a slice.
         */
        public enum SliceStatus {
            HEALTHY,
            DEGRADED,
            UNHEALTHY,
            STARTING,
            STOPPING,
            STOPPED,
            FAILED
        }
    }
}