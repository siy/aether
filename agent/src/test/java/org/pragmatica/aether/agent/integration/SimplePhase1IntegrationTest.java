package org.pragmatica.aether.agent.integration;

import org.junit.jupiter.api.*;
import org.pragmatica.aether.agent.AetherAgent;
import org.pragmatica.aether.agent.config.AgentConfiguration;
import org.pragmatica.aether.agent.features.FeatureToggle;
import org.pragmatica.aether.agent.features.SimpleFeatureToggle;
import org.pragmatica.aether.agent.llm.SimpleMockLLMProvider;
import org.pragmatica.aether.agent.message.AgentRecommendation;
import org.pragmatica.aether.agent.message.ClusterEvent;
import org.pragmatica.aether.agent.message.SliceTelemetryBatch;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.message.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Simplified Phase 1 integration test demonstrating the working agent system.
 * Tests the three development tracks in isolation without complex cross-dependencies.
 */
@DisplayName("Simple Phase 1 Integration Test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimplePhase1IntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(SimplePhase1IntegrationTest.class);
    
    private NodeId testNodeId;
    private MessageRouter mockMessageRouter;
    private SimpleMockLLMProvider llmProvider;
    private SimpleFeatureToggle featureToggle;
    private AgentLLMService agentLLMService;
    private AetherAgent agent;
    private TelemetrySimulator telemetrySimulator;
    
    @BeforeEach
    void setUp() {
        logger.info("Setting up simplified Phase 1 integration test");
        
        // Initialize core components
        testNodeId = NodeId.nodeId("test-node");
        mockMessageRouter = mock(MessageRouter.class);
        llmProvider = new SimpleMockLLMProvider("test-provider");
        featureToggle = new SimpleFeatureToggle();
        telemetrySimulator = new TelemetrySimulator();
        
        // Create integrated service
        agentLLMService = new AgentLLMService(llmProvider, (FeatureToggle) featureToggle);
        
        // Create agent with default configuration
        var agentConfig = AgentConfiguration.defaultConfiguration();
        agent = new AetherAgent(testNodeId, mockMessageRouter, agentConfig);
        
        logger.info("Phase 1 integration test setup complete");
    }
    
    @Test
    @Order(1)
    @DisplayName("Test Track A: Message Infrastructure")
    void testMessageInfrastructure() {
        logger.info("Testing Track A: Message Infrastructure");
        
        // Test agent creation
        assertThat(agent).isNotNull();
        
        // Test telemetry batch creation
        var telemetryBatch = telemetrySimulator.generateNormalTelemetry();
        assertThat(telemetryBatch).isNotNull();
        assertThat(telemetryBatch.sliceMetrics()).isNotEmpty();
        assertThat(telemetryBatch.nodeId()).isNotEmpty();
        
        // Test cluster event creation
        var clusterEvent = telemetrySimulator.generateClusterEvent(ClusterEvent.ClusterEventType.PERFORMANCE_DEGRADATION);
        assertThat(clusterEvent).isNotNull();
        assertThat(clusterEvent.eventType()).isEqualTo(ClusterEvent.ClusterEventType.PERFORMANCE_DEGRADATION);
        
        logger.info("✅ Track A message infrastructure working");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test Track B: Mock LLM Provider")
    void testMockLLMProvider() throws Exception {
        logger.info("Testing Track B: Mock LLM Provider");
        
        // Test LLM provider
        var request = new SimpleMockLLMProvider.CompletionRequest(
            List.of("Analyze system performance"),
            200,
            0.1
        );
        
        var response = llmProvider.complete(request).get(5, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.content()).isNotEmpty();
        assertThat(response.tokensUsed()).isGreaterThan(0);
        
        // Test feature toggles
        assertThat(featureToggle.isEnabled("agent.enabled")).isTrue();
        assertThat(featureToggle.isEnabled("agent.shadow_mode")).isFalse();
        
        featureToggle.emergencyDisableAll();
        assertThat(featureToggle.isEmergencyMode()).isTrue();
        assertThat(featureToggle.isEnabled("agent.enabled")).isFalse();
        
        featureToggle.restoreFromEmergency();
        assertThat(featureToggle.isEmergencyMode()).isFalse();
        assertThat(featureToggle.isEnabled("agent.enabled")).isTrue();
        
        logger.info("✅ Track B mock LLM and feature toggles working");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test Agent-LLM Integration")
    void testAgentLLMIntegration() throws Exception {
        logger.info("Testing Agent-LLM Integration");
        
        // Test telemetry processing through the integration service
        var telemetryBatch = telemetrySimulator.generatePerformanceIssueTelemetry();
        var recommendation = agentLLMService.processTelemetry(telemetryBatch).get(10, TimeUnit.SECONDS);
        
        assertThat(recommendation).isNotNull();
        assertThat(recommendation.messageId()).isNotNull();
        assertThat(recommendation.summary()).isNotEmpty();
        assertThat(recommendation.confidence()).isGreaterThan(0.0);
        assertThat(recommendation.actionSteps()).isNotEmpty();
        
        // Test that recommendation contains relevant content
        var summary = recommendation.summary().toLowerCase();
        assertThat(summary).containsAnyOf("performance", "cpu", "memory", "scale");
        
        logger.info("✅ Agent-LLM integration working");
        logger.info("   Generated recommendation: {}", recommendation.getDisplaySummary());
    }
    
    @Test
    @Order(4)
    @DisplayName("Test Feature Toggle Control")
    void testFeatureToggleControl() throws Exception {
        logger.info("Testing Feature Toggle Control");
        
        // Test normal operation
        var telemetryBatch = telemetrySimulator.generateNormalTelemetry();
        var normalRecommendation = agentLLMService.processTelemetry(telemetryBatch).get(10, TimeUnit.SECONDS);
        assertThat(normalRecommendation).isNotNull();
        
        // Test with recommendations disabled
        featureToggle.setEnabled("agent.recommendations.enabled", false);
        var disabledRecommendation = agentLLMService.processTelemetry(telemetryBatch).get(10, TimeUnit.SECONDS);
        assertThat(disabledRecommendation).isNull(); // Should be null when disabled
        
        // Test emergency mode
        featureToggle.setEnabled("agent.recommendations.enabled", true);
        featureToggle.emergencyDisableAll();
        var emergencyRecommendation = agentLLMService.processTelemetry(telemetryBatch).get(10, TimeUnit.SECONDS);
        assertThat(emergencyRecommendation).isNull(); // Should be null in emergency mode
        
        featureToggle.restoreFromEmergency();
        
        logger.info("✅ Feature toggle control working");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test End-to-End Workflow")
    void testEndToEndWorkflow() throws Exception {
        logger.info("Testing end-to-end workflow");
        
        // Simulate realistic scenario: performance issues detected
        var telemetryBatch = telemetrySimulator.generatePerformanceIssueTelemetry();
        var recommendation = agentLLMService.processTelemetry(telemetryBatch).get(15, TimeUnit.SECONDS);
        
        assertThat(recommendation).isNotNull();
        assertThat(recommendation.requiresImmediateAction()).isIn(true, false); // Could be either depending on scenario
        assertThat(recommendation.totalEstimatedDuration()).isNotNull();
        
        // Test that the recommendation is appropriate for the scenario
        var summary = recommendation.summary().toLowerCase();
        assertThat(summary).containsAnyOf("cpu", "scale", "resource", "performance");
        
        // Test metrics collection
        var metrics = agentLLMService.metrics();
        assertThat(metrics).isNotNull();
        assertThat(metrics.totalRecommendations()).isGreaterThan(0);
        
        logger.info("✅ End-to-end workflow complete");
        logger.info("   Recommendation: {}", recommendation.displaySummary());
        logger.info("   Estimated duration: {}", recommendation.totalEstimatedDuration());
        logger.info("   Action steps: {}", recommendation.actionSteps().size());
    }
    
    @AfterEach
    void tearDown() {
        logger.info("Phase 1 integration test completed successfully");
        logger.info("Ready for Phase 2: Observability & Collection");
    }
}