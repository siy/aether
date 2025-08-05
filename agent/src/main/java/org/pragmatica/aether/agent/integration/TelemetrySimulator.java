package org.pragmatica.aether.agent.integration;

import org.pragmatica.aether.agent.message.ClusterEvent;
import org.pragmatica.aether.agent.message.SliceTelemetryBatch;
import org.pragmatica.cluster.net.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Telemetry simulator for Phase 1 integration testing.
 * 
 * Generates realistic telemetry data and cluster events to test the complete
 * agent system without requiring actual cluster deployment. This supports
 * the demonstration of end-to-end functionality for the early demo milestone.
 * 
 * The simulator creates various scenarios:
 * - Normal operation telemetry
 * - Performance issues (high CPU, memory leaks)
 * - Scaling events and capacity issues
 * - Error conditions and failures
 * - Mixed scenarios for comprehensive testing
 * 
 * Data patterns are designed to trigger specific responses from the mock LLM
 * provider, demonstrating the complete recommendation generation pipeline.
 */
public class TelemetrySimulator {
    private static final Logger logger = LoggerFactory.getLogger(TelemetrySimulator.class);
    
    private final Random random;
    
    // Simulated slice names for realistic scenarios
    private static final List<String> SLICE_NAMES = List.of(
        "payment-service",
        "user-management", 
        "notification-service",
        "analytics-engine",
        "auth-service",
        "logging-service",
        "monitoring-service",
        "data-processor"
    );
    
    // Simulated node IDs
    private static final List<NodeId> NODE_IDS = List.of(
        NodeId.nodeId("node-1"),
        NodeId.nodeId("node-2"), 
        NodeId.nodeId("node-3"),
        NodeId.nodeId("node-4")
    );
    
    public TelemetrySimulator() {
        this.random = ThreadLocalRandom.current();
        logger.info("TelemetrySimulator initialized for Phase 1 integration testing");
    }
    
    /**
     * Generates a telemetry batch representing normal system operation.
     */
    public SliceTelemetryBatch generateNormalTelemetry() {
        var startTime = Instant.now().minusSeconds(300); // 5 minutes ago
        var endTime = Instant.now();
        
        var telemetryEntries = SLICE_NAMES.stream()
            .limit(random.nextInt(3, 6)) // 3-5 slices
            .collect(Collectors.toMap(
                sliceName -> sliceName,
                sliceName -> generateNormalMetrics()
            ));
        
        logger.debug("Generated normal telemetry batch with {} slices", telemetryEntries.size());
        
        // Convert map to SliceMetrics list
        var sliceMetrics = telemetryEntries.entrySet().stream()
            .map(entry -> createSliceMetrics(entry.getKey(), entry.getValue()))
            .toList();
        
        return SliceTelemetryBatch.create(
            NODE_IDS.get(random.nextInt(NODE_IDS.size())).toString(),
            sliceMetrics,
            Duration.ofSeconds(300),
            sliceMetrics.size() * 100L
        );
    }
    
    /**
     * Generates a telemetry batch with performance issues.
     */
    public SliceTelemetryBatch generatePerformanceIssueTelemetry() {
        var startTime = Instant.now().minusSeconds(300);
        var endTime = Instant.now();
        
        var problematicSlice = SLICE_NAMES.get(random.nextInt(SLICE_NAMES.size()));
        
        var telemetryEntries = SLICE_NAMES.stream()
            .limit(random.nextInt(4, 7))
            .collect(Collectors.toMap(
                sliceName -> sliceName,
                sliceName -> sliceName.equals(problematicSlice) ? 
                    generateHighCPUMetrics() : generateNormalMetrics()
            ));
        
        logger.debug("Generated performance issue telemetry with problems in {}", problematicSlice);
        
        // Convert map to SliceMetrics list
        var sliceMetrics = telemetryEntries.entrySet().stream()
            .map(entry -> createSliceMetrics(entry.getKey(), entry.getValue()))
            .toList();
        
        return SliceTelemetryBatch.create(
            NODE_IDS.get(random.nextInt(NODE_IDS.size())).toString(),
            sliceMetrics,
            Duration.ofSeconds(300),
            sliceMetrics.size() * 100L
        );
    }
    
    /**
     * Generates a telemetry batch with memory issues.
     */
    public SliceTelemetryBatch generateMemoryIssueTelemetry() {
        var startTime = Instant.now().minusSeconds(300);
        var endTime = Instant.now();
        
        var problematicSlice = SLICE_NAMES.get(random.nextInt(SLICE_NAMES.size()));
        
        var telemetryEntries = SLICE_NAMES.stream()
            .limit(random.nextInt(4, 6))
            .collect(Collectors.toMap(
                sliceName -> sliceName,
                sliceName -> sliceName.equals(problematicSlice) ?
                    generateMemoryLeakMetrics() : generateNormalMetrics()
            ));
        
        logger.debug("Generated memory issue telemetry with leak in {}", problematicSlice);
        
        // Convert map to SliceMetrics list
        var sliceMetrics = telemetryEntries.entrySet().stream()
            .map(entry -> createSliceMetrics(entry.getKey(), entry.getValue()))
            .toList();
        
        return SliceTelemetryBatch.create(
            NODE_IDS.get(random.nextInt(NODE_IDS.size())).toString(),
            sliceMetrics,
            Duration.ofSeconds(300),
            sliceMetrics.size() * 100L
        );
    }
    
    /**
     * Generates a telemetry batch indicating scaling needs.
     */
    public SliceTelemetryBatch generateScalingNeedsTelemetry() {
        var startTime = Instant.now().minusSeconds(300);
        var endTime = Instant.now();
        
        var highLoadSlice = SLICE_NAMES.get(random.nextInt(SLICE_NAMES.size()));
        
        var telemetryEntries = SLICE_NAMES.stream()
            .limit(random.nextInt(5, 7))
            .collect(Collectors.toMap(
                sliceName -> sliceName,
                sliceName -> sliceName.equals(highLoadSlice) ?
                    generateHighLoadMetrics() : generateNormalMetrics()
            ));
        
        logger.debug("Generated scaling needs telemetry with high load in {}", highLoadSlice);
        
        // Convert map to SliceMetrics list
        var sliceMetrics = telemetryEntries.entrySet().stream()
            .map(entry -> createSliceMetrics(entry.getKey(), entry.getValue()))
            .toList();
        
        return SliceTelemetryBatch.create(
            NODE_IDS.get(random.nextInt(NODE_IDS.size())).toString(),
            sliceMetrics,
            Duration.ofSeconds(300),
            sliceMetrics.size() * 100L
        );
    }
    
    /**
     * Generates a cluster event representing various system events.
     */
    public ClusterEvent generateClusterEvent(ClusterEvent.ClusterEventType eventType) {
        var nodeId = NODE_IDS.get(random.nextInt(NODE_IDS.size()));
        var severity = determineSeverity(eventType);
        var eventData = generateEventData(eventType, nodeId);
        
        var description = "Cluster event: " + eventType.toString().toLowerCase().replace('_', ' ');
        
        logger.debug("Generated {} cluster event on node {} with severity {}", 
                   eventType, nodeId, severity);
        
        return ClusterEvent.create(
            nodeId.toString(),
            eventType,
            description,
            eventData,
            severity
        );
    }
    
    /**
     * Generates a random cluster event.
     */
    public ClusterEvent generateRandomClusterEvent() {
        var eventTypes = ClusterEvent.ClusterEventType.values();
        var randomType = eventTypes[random.nextInt(eventTypes.length)];
        return generateClusterEvent(randomType);
    }
    
    /**
     * Generates a sequence of related telemetry batches for scenario testing.
     */
    public List<SliceTelemetryBatch> generateTelemetrySequence(TelemetryScenario scenario, int batchCount) {
        return switch (scenario) {
            case NORMAL_OPERATION -> 
                IntStream.range(0, batchCount)
                    .mapToObj(i -> generateNormalTelemetry())
                    .toList();
                    
            case PERFORMANCE_DEGRADATION ->
                IntStream.range(0, batchCount)
                    .mapToObj(i -> generatePerformanceIssueTelemetry())
                    .toList();
                    
            case MEMORY_ISSUES ->
                IntStream.range(0, batchCount)
                    .mapToObj(i -> generateMemoryIssueTelemetry())
                    .toList();
                    
            case SCALING_NEEDS ->
                IntStream.range(0, batchCount)
                    .mapToObj(i -> generateScalingNeedsTelemetry())
                    .toList();
                    
            case MIXED_SCENARIO -> {
                var batches = new ArrayList<SliceTelemetryBatch>();
                for (int i = 0; i < batchCount; i++) {
                    batches.add(switch (i % 4) {
                        case 0 -> generateNormalTelemetry();
                        case 1 -> generatePerformanceIssueTelemetry();
                        case 2 -> generateMemoryIssueTelemetry();
                        case 3 -> generateScalingNeedsTelemetry();
                        default -> generateNormalTelemetry();
                    });
                }
                yield List.copyOf(batches);
            }
        };
    }
    
    /**
     * Generates normal operating metrics.
     */
    private Map<String, Object> generateNormalMetrics() {
        return Map.of(
            "cpu_usage_percent", random.nextDouble(10.0, 30.0),
            "memory_usage_mb", random.nextInt(200, 400),
            "heap_usage_percent", random.nextDouble(20.0, 40.0),
            "request_count", random.nextInt(50, 200),
            "response_time_ms", random.nextDouble(10.0, 50.0),
            "error_count", random.nextInt(0, 3),
            "active_connections", random.nextInt(5, 20),
            "gc_time_ms", random.nextDouble(5.0, 15.0)
        );
    }
    
    /**
     * Generates high CPU usage metrics.
     */
    private Map<String, Object> generateHighCPUMetrics() {
        return Map.of(
            "cpu_usage_percent", random.nextDouble(80.0, 95.0), // High CPU
            "memory_usage_mb", random.nextInt(300, 500),
            "heap_usage_percent", random.nextDouble(40.0, 60.0),
            "request_count", random.nextInt(200, 500), // High load
            "response_time_ms", random.nextDouble(100.0, 300.0), // Slow responses
            "error_count", random.nextInt(5, 15), // More errors
            "active_connections", random.nextInt(30, 60),
            "gc_time_ms", random.nextDouble(20.0, 40.0)
        );
    }
    
    /**
     * Generates memory leak indicators.
     */
    private Map<String, Object> generateMemoryLeakMetrics() {
        return Map.of(
            "cpu_usage_percent", random.nextDouble(40.0, 60.0),
            "memory_usage_mb", random.nextInt(800, 1200), // High memory
            "heap_usage_percent", random.nextDouble(80.0, 95.0), // Near full heap
            "request_count", random.nextInt(100, 200),
            "response_time_ms", random.nextDouble(50.0, 150.0),
            "error_count", random.nextInt(2, 8),
            "active_connections", random.nextInt(15, 35),
            "gc_time_ms", random.nextDouble(40.0, 80.0), // High GC time
            "memory_leak_indicator", true // Explicit leak indicator
        );
    }
    
    /**
     * Generates high load metrics indicating scaling needs.
     */
    private Map<String, Object> generateHighLoadMetrics() {
        return Map.of(
            "cpu_usage_percent", random.nextDouble(60.0, 80.0),
            "memory_usage_mb", random.nextInt(400, 600),
            "heap_usage_percent", random.nextDouble(50.0, 70.0),
            "request_count", random.nextInt(400, 800), // Very high load
            "response_time_ms", random.nextDouble(80.0, 200.0),
            "error_count", random.nextInt(3, 10),
            "active_connections", random.nextInt(40, 80), // Many connections
            "gc_time_ms", random.nextDouble(15.0, 30.0),
            "queue_depth", random.nextInt(50, 150), // Backed up requests
            "replica_count", random.nextInt(2, 4) // Current replicas
        );
    }
    
    /**
     * Determines event severity based on type.
     */
    private ClusterEvent.Severity determineSeverity(ClusterEvent.ClusterEventType eventType) {
        return switch (eventType) {
            case SYSTEM_ERROR, RESOURCE_EXHAUSTION -> ClusterEvent.Severity.CRITICAL;
            case PERFORMANCE_DEGRADATION, HEALTH_CHECK_FAILURE -> ClusterEvent.Severity.HIGH;
            case TOPOLOGY_CHANGE, DEPLOYMENT_CHANGE -> ClusterEvent.Severity.MEDIUM;
            case APPLICATION_EVENT, CONFIGURATION_CHANGE -> ClusterEvent.Severity.LOW;
            default -> ClusterEvent.Severity.INFO;
        };
    }
    
    /**
     * Creates SliceMetrics from the old format.
     */
    private SliceTelemetryBatch.SliceMetrics createSliceMetrics(String sliceName, Map<String, Object> metrics) {
        var performance = new SliceTelemetryBatch.SliceMetrics.PerformanceMetrics(
            ((Number) metrics.getOrDefault("response_time_ms", 50.0)).doubleValue(),
            ((Number) metrics.getOrDefault("response_time_ms", 50.0)).doubleValue() * 1.5,
            ((Number) metrics.getOrDefault("response_time_ms", 50.0)).doubleValue() * 2.0,
            ((Number) metrics.getOrDefault("request_count", 100)).longValue(),
            ((Number) metrics.getOrDefault("error_count", 0)).longValue(),
            ((Number) metrics.getOrDefault("error_count", 0)).doubleValue() / 
                Math.max(1, ((Number) metrics.getOrDefault("request_count", 100)).doubleValue()),
            ((Number) metrics.getOrDefault("request_count", 100)).doubleValue() / 300.0
        );
        
        var resources = new SliceTelemetryBatch.SliceMetrics.ResourceMetrics(
            ((Number) metrics.getOrDefault("cpu_usage_percent", 25.0)).doubleValue(),
            ((Number) metrics.getOrDefault("memory_usage_mb", 300)).longValue() * 1024 * 1024,
            1024L * 1024 * 1024, // 1GB max
            ((Number) metrics.getOrDefault("heap_usage_percent", 30.0)).doubleValue(),
            random.nextLong(1000000, 10000000), // Network bytes in
            random.nextLong(1000000, 10000000), // Network bytes out
            random.nextLong(100000, 1000000),   // Disk bytes read
            random.nextLong(100000, 1000000)    // Disk bytes written
        );
        
        var operations = new SliceTelemetryBatch.SliceMetrics.OperationalMetrics(
            SliceTelemetryBatch.SliceMetrics.SliceStatus.HEALTHY,
            random.nextInt(2, 5), // replica count
            random.nextInt(2, 5), // healthy replicas
            random.nextLong(3600, 86400), // uptime
            0, // restart count
            Map.of("custom_metric", "value")
        );
        
        return new SliceTelemetryBatch.SliceMetrics(
            sliceName + "-" + random.nextInt(100, 999),
            sliceName,
            performance,
            resources,
            operations
        );
    }

    /**
     * Generates event-specific data.
     */
    private Map<String, Object> generateEventData(ClusterEvent.ClusterEventType eventType, NodeId nodeId) {
        return switch (eventType) {
            case SYSTEM_ERROR -> Map.of(
                "errorType", "OutOfMemoryError",
                "errorMessage", "Java heap space",
                "affectedSlices", random.nextInt(1, 4)
            );
            
            case RESOURCE_EXHAUSTION -> Map.of(
                "resourceType", "memory",
                "currentUsage", random.nextDouble(90.0, 98.0),
                "threshold", 85.0,
                "affectedServices", List.of(SLICE_NAMES.get(random.nextInt(SLICE_NAMES.size())))
            );
            
            case PERFORMANCE_DEGRADATION -> Map.of(
                "metricName", "response_time",
                "currentValue", random.nextDouble(200.0, 500.0),
                "threshold", 100.0,
                "duration", random.nextInt(300, 900) + "s"
            );
            
            case DEPLOYMENT_CHANGE -> Map.of(
                "sliceName", SLICE_NAMES.get(random.nextInt(SLICE_NAMES.size())),
                "currentReplicas", random.nextInt(2, 5),
                "targetReplicas", random.nextInt(5, 10),
                "trigger", "cpu_utilization > 80%"
            );
            
            case TOPOLOGY_CHANGE -> Map.of(
                "changeType", "node_join",
                "newNodeId", nodeId.toString(),
                "clusterSize", random.nextInt(3, 8)
            );
            
            case HEALTH_CHECK_FAILURE -> Map.of(
                "checkType", "deep_health_check",
                "failedChecks", random.nextInt(1, 5),
                "totalChecks", random.nextInt(8, 15)
            );
            
            case APPLICATION_EVENT -> Map.of(
                "eventName", "user_spike",
                "description", "Sudden increase in user activity",
                "impact", "moderate"
            );
            
            default -> Map.of(
                "description", "Generic cluster event",
                "timestamp", Instant.now().toString()
            );
        };
    }
    
    /**
     * Telemetry scenario types for testing.
     */
    public enum TelemetryScenario {
        NORMAL_OPERATION,
        PERFORMANCE_DEGRADATION,
        MEMORY_ISSUES,
        SCALING_NEEDS,
        MIXED_SCENARIO
    }
}