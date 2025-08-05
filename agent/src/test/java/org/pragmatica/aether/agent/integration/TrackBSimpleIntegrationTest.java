package org.pragmatica.aether.agent.integration;

import org.junit.jupiter.api.*;
import org.pragmatica.aether.agent.features.FeatureToggle;
import org.pragmatica.aether.agent.features.SimpleFeatureToggle;
import org.pragmatica.aether.agent.llm.SimpleLLMProvider;
import org.pragmatica.aether.agent.llm.SimpleMockLLMProvider;
import org.pragmatica.lang.Result.Success;
import org.pragmatica.lang.Result.Failure;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.pragmatica.aether.agent.features.FeatureToggle.KnownFeature.*;

/**
 * Comprehensive integration tests for Track B implementation.
 * 
 * These tests validate the complete Mock LLM Provider system and Feature Toggle
 * framework working together. The tests demonstrate:
 * - End-to-end functionality without cloud dependencies
 * - Feature toggle runtime configuration  
 * - Mock provider deterministic responses
 * - Progressive testing capabilities
 */
@DisplayName("Track B Simple Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TrackBSimpleIntegrationTest {
    
    private SimpleMockLLMProvider llmProvider;
    private SimpleFeatureToggle featureToggle;
    
    @BeforeEach
    void setUp() {
        llmProvider = new SimpleMockLLMProvider("integration-test-provider");
        featureToggle = new SimpleFeatureToggle();
    }
    
    @Test
    @Order(1)
    @DisplayName("Verify basic system initialization")
    void testSystemInitialization() {
        // Verify LLM provider is initialized
        assertThat(llmProvider.providerId()).isEqualTo("integration-test-provider");
        
        // Verify feature toggles have correct defaults
        assertThat(((FeatureToggle) featureToggle).isEnabled(AGENT_ENABLED)).isTrue();
        assertThat(((FeatureToggle) featureToggle).isEnabled(LLM_LOCAL_ENABLED)).isTrue();
        assertThat(((FeatureToggle) featureToggle).isEnabled(LLM_CLOUD_ENABLED)).isFalse();
        assertThat(featureToggle.isEmergencyMode()).isFalse();
    }
    
    @Test
    @Order(2)
    @DisplayName("Test MockLLMProvider deterministic responses")
    void testMockProviderDeterministicResponses() throws Exception {
        // Test performance analysis scenario
        var performanceRequest = new SimpleLLMProvider.CompletionRequest(
            List.of("The CPU usage is high on node-1"),
            200,
            0.0
        );
        
        var response1 = switch(llmProvider.complete(performanceRequest).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        var response2 = switch(llmProvider.complete(performanceRequest).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        
        // Responses should be identical (deterministic)
        assertThat(response1.content()).isEqualTo(response2.content());
        assertThat(response1.tokensUsed()).isEqualTo(response2.tokensUsed());
        
        // Verify the response contains expected performance analysis
        var content = response1.content();
        assertThat(content.toLowerCase()).containsAnyOf("cpu", "performance", "recommend");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test scenario pattern matching")
    void testScenarioPatternMatching() throws Exception {
        var testCases = List.of(
            // Performance scenario
            new TestCase("performance analysis", "High CPU usage on payment service", "cpu"),
            
            // Error scenario  
            new TestCase("error analysis", "Multiple exceptions in authentication service", "error"),
            
            // Memory scenario
            new TestCase("memory analysis", "Memory leak detected in user sessions", "memory"),
            
            // Scaling scenario
            new TestCase("scaling analysis", "Need to scale replicas for high traffic", "scal")
        );
        
        for (var testCase : testCases) {
            var request = new SimpleLLMProvider.CompletionRequest(
                List.of(testCase.input),
                200,
                0.0
            );
            
            var response = switch(llmProvider.complete(request).await()) {
                case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
                case Failure f -> {
                    throw new RuntimeException(f.cause().message());
                }
            };
            
            var content = response.content().toLowerCase();
            assertThat(content).describedAs("Response for " + testCase.description)
                .contains(testCase.expectedKeyword);
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("Test feature toggle runtime configuration")
    void testFeatureToggleRuntimeConfiguration() {
        // Initially LLM should be enabled
        assertThat(((FeatureToggle) featureToggle).isEnabled(LLM_LOCAL_ENABLED)).isTrue();
        
        // Disable LLM features
        ((FeatureToggle) featureToggle).setEnabled(LLM_LOCAL_ENABLED, false);
        assertThat(((FeatureToggle) featureToggle).isEnabled(LLM_LOCAL_ENABLED)).isFalse();
        
        // Re-enable
        ((FeatureToggle) featureToggle).setEnabled(LLM_LOCAL_ENABLED, true);
        assertThat(((FeatureToggle) featureToggle).isEnabled(LLM_LOCAL_ENABLED)).isTrue();
    }
    
    @Test
    @Order(5)
    @DisplayName("Test emergency mode functionality")
    void testEmergencyMode() throws Exception {
        // Initially features should be enabled
        assertThat(((FeatureToggle) featureToggle).isEnabled(LLM_LOCAL_ENABLED)).isTrue();
        assertThat(featureToggle.isEmergencyMode()).isFalse();
        
        // Test that LLM works normally
        var request = new SimpleLLMProvider.CompletionRequest(
            List.of("Test request before emergency"),
            100,
            0.0
        );
        
        var normalResponse = switch(llmProvider.complete(request).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        assertThat(normalResponse.content()).isNotEmpty();
        
        // Activate emergency mode
        featureToggle.emergencyDisableAll();
        
        // All features should now be disabled
        assertThat(featureToggle.isEmergencyMode()).isTrue();
        assertThat(((FeatureToggle) featureToggle).isEnabled(LLM_LOCAL_ENABLED)).isFalse();
        assertThat(((FeatureToggle) featureToggle).isEnabled(AGENT_ENABLED)).isFalse();
        
        // In a real system, this would prevent LLM requests
        // For this test, we just verify the toggle state
        
        // Restore from emergency mode
        featureToggle.restoreFromEmergency();
        
        assertThat(featureToggle.isEmergencyMode()).isFalse();
        assertThat(((FeatureToggle) featureToggle).isEnabled(LLM_LOCAL_ENABLED)).isTrue();
    }
    
    @Test
    @Order(6)
    @DisplayName("Test feature toggle integration with LLM decisions")
    void testFeatureToggleLLMIntegration() throws Exception {
        // Simulate a system that checks feature toggles before making LLM calls
        var llmIntegratedSystem = new LLMIntegratedSystem(llmProvider, featureToggle);
        
        // Normal operation - should work
        var response1 = llmIntegratedSystem.processRequest("Analyze system performance");
        assertThat(response1).contains("performance");
        
        // Disable local LLM
        ((FeatureToggle) featureToggle).setEnabled(LLM_LOCAL_ENABLED, false);
        
        // Should return fallback response
        var response2 = llmIntegratedSystem.processRequest("Analyze system performance");
        assertThat(response2).contains("LLM features are disabled");
        
        // Re-enable local LLM
        ((FeatureToggle) featureToggle).setEnabled(LLM_LOCAL_ENABLED, true);
        
        // Should work again
        var response3 = llmIntegratedSystem.processRequest("Analyze system performance");
        assertThat(response3).contains("performance");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test cost tracking and monitoring")
    void testCostTrackingAndMonitoring() throws Exception {
        var costTracker = new SimpleCostTracker();
        
        // Make several requests and track costs
        for (int i = 0; i < 5; i++) {
            var request = new SimpleLLMProvider.CompletionRequest(
                List.of("Cost tracking test " + i),
                100,
                0.0
            );
            
            var response = switch(llmProvider.complete(request).await()) {
                case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
                case Failure f -> {
                    throw new RuntimeException(f.cause().message());
                }
            };
            costTracker.recordCost(response.cost());
        }
        
        // Verify costs were tracked
        assertThat(costTracker.getTotalCost()).isGreaterThan(0.0);
        assertThat(costTracker.getRequestCount()).isEqualTo(5);
    }
    
    @Test
    @Order(8)
    @DisplayName("Test progressive testing capabilities")
    void testProgressiveTestingCapabilities() throws Exception {
        // This test demonstrates the progressive testing approach:
        // Phase 1: Mock provider only (current implementation)
        var mockResponse = performTestRequest("Phase 1: Mock provider test");
        assertThat(mockResponse.modelUsed()).contains("mock");
        
        // Verify the mock provider provides realistic responses
        assertThat(mockResponse.content()).isNotEmpty();
        assertThat(mockResponse.content().length()).isGreaterThan(10);
        
        // Verify response metadata contains expected fields
        var metadata = mockResponse.metadata();
        assertThat(metadata).containsKeys("scenario", "requestId", "providerId");
        
        // Phase 2 would involve local providers (not implemented yet)
        // Phase 3 would involve cloud providers (not implemented yet)
        
        System.out.println("✓ Phase 1 (Mock Provider) - Completed");
        System.out.println("  - Deterministic responses: Working");
        System.out.println("  - Scenario matching: Working");  
        System.out.println("  - Cost tracking: Working");
        System.out.println("  - Feature toggles: Working");
        System.out.println("  - Emergency mode: Working");
        System.out.println("○ Phase 2 (Local Providers) - Not implemented");
        System.out.println("○ Phase 3 (Cloud Providers) - Not implemented");
    }
    
    private SimpleLLMProvider.CompletionResponse performTestRequest(String message) throws Exception {
        var request = new SimpleLLMProvider.CompletionRequest(
            List.of(message),
            150,
            0.0
        );
        
        return switch(llmProvider.complete(request).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
    }
    
    /**
     * Helper record for test cases.
     */
    private record TestCase(String description, String input, String expectedKeyword) {}
    
    /**
     * Simple system that integrates LLM with feature toggles.
     */
    private static class LLMIntegratedSystem {
        private final SimpleMockLLMProvider llmProvider;
        private final SimpleFeatureToggle featureToggle;
        
        LLMIntegratedSystem(SimpleMockLLMProvider llmProvider, SimpleFeatureToggle featureToggle) {
            this.llmProvider = llmProvider;
            this.featureToggle = featureToggle;
        }
        
        String processRequest(String input) throws Exception {
            if (!((FeatureToggle) featureToggle).isEnabled(LLM_LOCAL_ENABLED)) {
                return "LLM features are disabled - using fallback response";
            }
            
            var request = new SimpleLLMProvider.CompletionRequest(
                List.of(input),
                100,
                0.0
            );
            
            var response = switch(llmProvider.complete(request).await()) {
                case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
                case Failure f -> {
                    throw new RuntimeException(f.cause().message());
                }
            };
            return response.content();
        }
    }
    
    /**
     * Simple cost tracker for demonstration.
     */
    private static class SimpleCostTracker {
        private double totalCost = 0.0;
        private int requestCount = 0;
        
        void recordCost(double cost) {
            totalCost += cost;
            requestCount++;
        }
        
        double getTotalCost() {
            return totalCost;
        }
        
        int getRequestCount() {
            return requestCount;
        }
    }
}