package org.pragmatica.aether.agent.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for the AgentMessage hierarchy.
 * Tests cover all message types, their creation patterns, and behavior validation.
 */
@DisplayName("AgentMessage Hierarchy Tests")
class AgentMessageTest {
    
    @Nested
    @DisplayName("SliceTelemetryBatch Tests")
    class SliceTelemetryBatchTest {
        
        @Test
        @DisplayName("Should create telemetry batch with default priority")
        void shouldCreateTelemetryBatchWithDefaultPriority() {
            var sliceMetrics = createSampleSliceMetrics();
            var batch = SliceTelemetryBatch.create(
                "node-1",
                List.of(sliceMetrics),
                Duration.ofSeconds(30),
                1000L
            );
            
            assertThat(batch.messageId()).isNotBlank();
            assertThat(batch.timestamp()).isGreaterThan(0);
            assertThat(batch.priority()).isEqualTo(AgentMessage.MessagePriority.LOW);
            assertThat(batch.nodeId()).isEqualTo("node-1");
            assertThat(batch.sliceMetrics()).hasSize(1);
            assertThat(batch.batchInterval()).isEqualTo(Duration.ofSeconds(30));
            assertThat(batch.totalEvents()).isEqualTo(1000L);
        }
        
        @Test
        @DisplayName("Should create valid slice metrics with all components")
        void shouldCreateValidSliceMetricsWithAllComponents() {
            var sliceMetrics = createSampleSliceMetrics();
            
            assertThat(sliceMetrics.sliceId()).isEqualTo("payment-service-v1");
            assertThat(sliceMetrics.sliceName()).isEqualTo("Payment Service");
            
            // Performance metrics validation
            var performance = sliceMetrics.performance();
            assertThat(performance.averageLatencyMs()).isEqualTo(45.5);
            assertThat(performance.p95LatencyMs()).isEqualTo(120.0);
            assertThat(performance.errorRate()).isEqualTo(0.02);
            
            // Resource metrics validation
            var resources = sliceMetrics.resources();
            assertThat(resources.cpuUtilizationPercent()).isEqualTo(65.5);
            assertThat(resources.memoryUtilizationPercent()).isEqualTo(78.2);
            
            // Operational metrics validation
            var operations = sliceMetrics.operations();
            assertThat(operations.status()).isEqualTo(SliceTelemetryBatch.SliceMetrics.SliceStatus.HEALTHY);
            assertThat(operations.replicaCount()).isEqualTo(3);
            assertThat(operations.healthyReplicas()).isEqualTo(3);
        }
        
        private SliceTelemetryBatch.SliceMetrics createSampleSliceMetrics() {
            var performance = new SliceTelemetryBatch.SliceMetrics.PerformanceMetrics(
                45.5, 120.0, 200.0, 10000L, 200L, 0.02, 167.5
            );
            
            var resources = new SliceTelemetryBatch.SliceMetrics.ResourceMetrics(
                65.5, 1024L * 1024 * 512, 1024L * 1024 * 1024, 78.2,
                1024L * 100, 1024L * 80, 1024L * 50, 1024L * 30
            );
            
            var operations = new SliceTelemetryBatch.SliceMetrics.OperationalMetrics(
                SliceTelemetryBatch.SliceMetrics.SliceStatus.HEALTHY,
                3, 3, 86400L, 0, Map.of("custom_metric", 42)
            );
            
            return new SliceTelemetryBatch.SliceMetrics(
                "payment-service-v1",
                "Payment Service",
                performance,
                resources,
                operations
            );
        }
    }
    
    @Nested
    @DisplayName("ClusterEvent Tests")
    class ClusterEventTest {
        
        @Test
        @DisplayName("Should create cluster event with appropriate priority mapping")
        void shouldCreateClusterEventWithAppropriatePriorityMapping() {
            var criticalEvent = ClusterEvent.create(
                "node-1",
                ClusterEvent.ClusterEventType.SYSTEM_ERROR,
                "Critical system failure",
                Map.of("error", "OutOfMemoryError"),
                ClusterEvent.Severity.CRITICAL
            );
            
            assertThat(criticalEvent.priority()).isEqualTo(AgentMessage.MessagePriority.CRITICAL);
            assertThat(criticalEvent.severity()).isEqualTo(ClusterEvent.Severity.CRITICAL);
            
            var infoEvent = ClusterEvent.create(
                "node-2",
                ClusterEvent.ClusterEventType.CONFIGURATION_CHANGE,
                "Configuration updated",
                Map.of("config", "logging.level"),
                ClusterEvent.Severity.INFO
            );
            
            assertThat(infoEvent.priority()).isEqualTo(AgentMessage.MessagePriority.LOW);
            assertThat(infoEvent.severity()).isEqualTo(ClusterEvent.Severity.INFO);
        }
        
        @Test
        @DisplayName("Should correctly identify immediate attention events")
        void shouldCorrectlyIdentifyImmediateAttentionEvents() {
            var criticalEvent = ClusterEvent.create(
                "node-1", ClusterEvent.ClusterEventType.SYSTEM_ERROR,
                "System failure", Map.of(), ClusterEvent.Severity.CRITICAL
            );
            
            var highEvent = ClusterEvent.create(
                "node-1", ClusterEvent.ClusterEventType.PERFORMANCE_DEGRADATION,
                "Performance issue", Map.of(), ClusterEvent.Severity.HIGH
            );
            
            var lowEvent = ClusterEvent.create(
                "node-1", ClusterEvent.ClusterEventType.CONFIGURATION_CHANGE,
                "Config change", Map.of(), ClusterEvent.Severity.LOW
            );
            
            assertThat(criticalEvent.requiresImmediateAttention()).isTrue();
            assertThat(highEvent.requiresImmediateAttention()).isTrue();
            assertThat(lowEvent.requiresImmediateAttention()).isFalse();
        }
        
        @Test
        @DisplayName("Should correctly categorize health-related events")
        void shouldCorrectlyCategorizeHealthRelatedEvents() {
            var healthEvent = ClusterEvent.create(
                "node-1", ClusterEvent.ClusterEventType.HEALTH_CHECK_FAILURE,
                "Health check failed", Map.of(), ClusterEvent.Severity.HIGH
            );
            
            var topologyEvent = ClusterEvent.create(
                "node-1", ClusterEvent.ClusterEventType.TOPOLOGY_CHANGE,
                "Node joined", Map.of(), ClusterEvent.Severity.INFO
            );
            
            assertThat(healthEvent.isHealthRelated()).isTrue();
            assertThat(healthEvent.isTopologyRelated()).isFalse();
            assertThat(topologyEvent.isHealthRelated()).isFalse();
            assertThat(topologyEvent.isTopologyRelated()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("StateTransition Tests")
    class StateTransitionTest {
        
        @Test
        @DisplayName("Should create state transition with correct priority based on type")
        void shouldCreateStateTransitionWithCorrectPriorityBasedOnType() {
            var failureTransition = StateTransition.create(
                "slice-1",
                StateTransition.TransitionType.SLICE_FAILURE,
                "HEALTHY",
                "FAILED",
                Map.of("cause", "OutOfMemoryError"),
                "Memory exhaustion"
            );
            
            assertThat(failureTransition.priority()).isEqualTo(AgentMessage.MessagePriority.CRITICAL);
            assertThat(failureTransition.isFailureTransition()).isTrue();
            assertThat(failureTransition.isRecoveryTransition()).isFalse();
            
            var routineTransition = StateTransition.create(
                "slice-1",
                StateTransition.TransitionType.ROUTINE_MAINTENANCE,
                "RUNNING",
                "MAINTENANCE",
                Map.of(),
                "Scheduled maintenance"
            );
            
            assertThat(routineTransition.priority()).isEqualTo(AgentMessage.MessagePriority.LOW);
            assertThat(routineTransition.isFailureTransition()).isFalse();
        }
        
        @Test
        @DisplayName("Should provide meaningful transition summary")
        void shouldProvidemeaningfulTransitionSummary() {
            var transition = StateTransition.create(
                "payment-service",
                StateTransition.TransitionType.SLICE_SCALING,
                "3-replicas",
                "5-replicas",
                Map.of("scaling", "up"),
                "Increased load detected"
            );
            
            String summary = transition.transitionSummary();
            assertThat(summary).isEqualTo("payment-service: 3-replicas -> 5-replicas (Increased load detected)");
            assertThat(transition.isScalingTransition()).isTrue();
        }
        
        @Test
        @DisplayName("Should calculate age correctly")
        void shouldCalculateAgeCorrectly() throws InterruptedException {
            var transition = StateTransition.create(
                "test-component",
                StateTransition.TransitionType.SLICE_STARTUP,
                "STOPPED",
                "STARTING",
                Map.of(),
                "Manual start"
            );
            
            Thread.sleep(10); // Small delay to ensure age > 0
            assertThat(transition.ageMillis()).isGreaterThan(0);
        }
    }
    
    @Nested
    @DisplayName("AgentRecommendation Tests")
    class AgentRecommendationTest {
        
        @Test
        @DisplayName("Should create recommendation with correct priority mapping")
        void shouldCreateRecommendationWithCorrectPriorityMapping() {
            var actionSteps = List.of(
                new AgentRecommendation.ActionStep(
                    1, "Scale up replicas", 
                    AgentRecommendation.ActionStep.ActionType.COMMAND_EXECUTION,
                    "scale --replicas=5 payment-service",
                    Map.of("replicas", "5"),
                    Duration.ofMinutes(2),
                    List.of(),
                    "scale --replicas=3 payment-service"
                )
            );
            
            var recommendation = AgentRecommendation.create(
                "analysis-123",
                AgentRecommendation.RecommendationType.SCALING_RECOMMENDATION,
                "Scale payment service to handle increased load",
                "CPU utilization consistently above 80% for 10 minutes",
                0.92,
                actionSteps,
                Map.of("cpu_avg", 85.5, "memory_avg", 65.2),
                Duration.ofMinutes(5),
                List.of("payment-service"),
                AgentRecommendation.RiskLevel.LOW
            );
            
            assertThat(recommendation.priority()).isEqualTo(AgentMessage.MessagePriority.LOW);
            assertThat(recommendation.confidence()).isEqualTo(0.92);
            assertThat(recommendation.canBeAutomated()).isTrue(); // High confidence, low risk, automatable actions
            assertThat(recommendation.requiresImmediateAction()).isFalse();
        }
        
        @Test
        @DisplayName("Should correctly assess automation eligibility")
        void shouldCorrectlyAssessAutomationEligibility() {
            var autoActionSteps = List.of(
                new AgentRecommendation.ActionStep(
                    1, "Update config", 
                    AgentRecommendation.ActionStep.ActionType.CONFIGURATION_UPDATE,
                    "update-config memory.max=2GB",
                    Map.of("memory.max", "2GB"),
                    Duration.ofSeconds(30),
                    List.of(),
                    "update-config memory.max=1GB"
                )
            );
            
            var manualActionSteps = List.of(
                new AgentRecommendation.ActionStep(
                    1, "Investigate issue", 
                    AgentRecommendation.ActionStep.ActionType.MANUAL_INVESTIGATION,
                    "check-logs --component=payment",
                    Map.of(),
                    Duration.ofMinutes(15),
                    List.of(),
                    ""
                )
            );
            
            var highConfidenceAutoRecommendation = AgentRecommendation.create(
                "analysis-1",
                AgentRecommendation.RecommendationType.CONFIGURATION_OPTIMIZATION,
                "Optimize memory settings",
                "Memory usage pattern analysis",
                0.95, // High confidence
                autoActionSteps,
                Map.of(),
                Duration.ofMinutes(1),
                List.of("service-1"),
                AgentRecommendation.RiskLevel.LOW
            );
            
            var lowConfidenceRecommendation = AgentRecommendation.create(
                "analysis-2",
                AgentRecommendation.RecommendationType.INVESTIGATION_REQUIRED,
                "Investigate performance issue",
                "Unclear performance degradation",
                0.65, // Low confidence
                manualActionSteps,
                Map.of(),
                Duration.ofMinutes(15),
                List.of("service-1"),
                AgentRecommendation.RiskLevel.MEDIUM
            );
            
            assertThat(highConfidenceAutoRecommendation.canBeAutomated()).isTrue();
            assertThat(lowConfidenceRecommendation.canBeAutomated()).isFalse();
        }
        
        @Test
        @DisplayName("Should calculate total estimated duration correctly")
        void shouldCalculateTotalEstimatedDurationCorrectly() {
            var actionSteps = List.of(
                new AgentRecommendation.ActionStep(
                    1, "Step 1", 
                    AgentRecommendation.ActionStep.ActionType.COMMAND_EXECUTION,
                    "command1", Map.of(), Duration.ofMinutes(2), List.of(), ""
                ),
                new AgentRecommendation.ActionStep(
                    2, "Step 2", 
                    AgentRecommendation.ActionStep.ActionType.VALIDATION_CHECK,
                    "validate", Map.of(), Duration.ofMinutes(3), List.of(), ""
                )
            );
            
            var recommendation = AgentRecommendation.create(
                "analysis-123",
                AgentRecommendation.RecommendationType.SCALING_RECOMMENDATION,
                "Multi-step recommendation",
                "Test reasoning",
                0.8,
                actionSteps,
                Map.of(),
                Duration.ofMinutes(5),
                List.of("service-1"),
                AgentRecommendation.RiskLevel.MEDIUM
            );
            
            assertThat(recommendation.expectedImpactTime()).isEqualTo(Duration.ofMinutes(5));
        }
        
        @Test
        @DisplayName("Should provide display summary")
        void shouldProvideDisplaySummary() {
            var recommendation = AgentRecommendation.create(
                "analysis-123",
                AgentRecommendation.RecommendationType.SCALING_RECOMMENDATION,
                "Scale payment service",
                "High load detected",
                0.87,
                List.of(),
                Map.of(),
                Duration.ofMinutes(5),
                List.of("payment-service"),
                AgentRecommendation.RiskLevel.LOW
            );
            
            String displaySummary = recommendation.summary();
            assertThat(displaySummary).isEqualTo("[SCALING_RECOMMENDATION] Scale payment service (Confidence: 87.0%, Risk: LOW)");
        }
        
        @Test
        @DisplayName("Should assess relevance based on age")
        void shouldAssessRelevanceBasedOnAge() throws InterruptedException {
            var recommendation = AgentRecommendation.create(
                "analysis-123",
                AgentRecommendation.RecommendationType.SCALING_RECOMMENDATION,
                "Scale service",
                "Test reasoning",
                0.8,
                List.of(),
                Map.of(),
                Duration.ofMinutes(5),
                List.of("service-1"),
                AgentRecommendation.RiskLevel.LOW
            );
            
            assertThat(recommendation.isStillRelevant(Duration.ofMinutes(10))).isTrue();
            
            Thread.sleep(10); // Small delay
            assertThat(recommendation.isStillRelevant(Duration.ofMillis(5))).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Message Priority Tests")
    class MessagePriorityTest {
        
        @Test
        @DisplayName("Should maintain correct priority ordering")
        void shouldMaintainCorrectPriorityOrdering() {
            var priorities = AgentMessage.MessagePriority.values();
            
            assertThat(priorities[0]).isEqualTo(AgentMessage.MessagePriority.CRITICAL);
            assertThat(priorities[1]).isEqualTo(AgentMessage.MessagePriority.HIGH);
            assertThat(priorities[2]).isEqualTo(AgentMessage.MessagePriority.NORMAL);
            assertThat(priorities[3]).isEqualTo(AgentMessage.MessagePriority.LOW);
        }
    }
}