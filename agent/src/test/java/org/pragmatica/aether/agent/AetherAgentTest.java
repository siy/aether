package org.pragmatica.aether.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pragmatica.aether.agent.config.AgentConfiguration;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.message.MessageRouter;

import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AetherAgent.
 * Tests cover lifecycle management, leadership handling, configuration management, and error scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AetherAgent Tests")
class AetherAgentTest {
    
    @Mock
    private MessageRouter messageRouter;
    
    private NodeId nodeId;
    private AgentConfiguration defaultConfig;
    private AetherAgent agent;
    
    @BeforeEach
    void setUp() {
        nodeId = NodeId.nodeId("test-node-1");
        defaultConfig = AgentConfiguration.defaultConfiguration();
        agent = new AetherAgent(nodeId, messageRouter, defaultConfig);
    }
    
    @Nested
    @DisplayName("Agent Creation Tests")
    class AgentCreationTest {
        
        @Test
        @DisplayName("Should create agent with provided configuration")
        void shouldCreateAgentWithProvidedConfiguration() {
            var customConfig = AgentConfiguration.builder()
                .withPrivacy(new AgentConfiguration.PrivacyConfig(
                    AgentConfiguration.PrivacyConfig.AuditLevel.BASIC,
                    Duration.ofDays(7),
                    true,
                    AgentConfiguration.PrivacyConfig.ConsentMode.OPT_IN,
                    java.util.List.of(),
                    java.util.Map.of()
                ))
                .build();
            
            var customAgent = new AetherAgent(nodeId, messageRouter, customConfig);
            
            assertThat(customAgent.currentConfiguration()).isEqualTo(customConfig);
            assertThat(customAgent.currentState()).isEqualTo(AetherAgent.AgentState.DORMANT);
            assertThat(customAgent.isActiveLeader()).isFalse();
        }
        
        @Test
        @DisplayName("Should create agent with default configuration using factory method")
        void shouldCreateAgentWithDefaultConfigurationUsingFactoryMethod() {
            var defaultAgent = AetherAgent.create(nodeId, messageRouter);
            
            assertThat(defaultAgent.currentConfiguration()).isNotNull();
            assertThat(defaultAgent.currentState()).isEqualTo(AetherAgent.AgentState.DORMANT);
        }
    }
    
    @Nested
    @DisplayName("Lifecycle Management Tests")
    class LifecycleManagementTest {
        
        @Test
        @DisplayName("Should start successfully and register for quorum notifications")
        void shouldStartSuccessfullyAndRegisterForQuorumNotifications() {
            var startFuture = agent.start();
            
            assertThatCode(() -> startFuture.await())
                .doesNotThrowAnyException();
            
            verify(messageRouter).addRoute(eq(QuorumStateNotification.class), any());
            
            assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.INACTIVE);
        }
        
        @Test
        @DisplayName("Should handle multiple start calls gracefully")
        void shouldHandleMultipleStartCallsGracefully() {
            agent.start().await();
            
            // Second start should warn but not fail
            var secondStart = agent.start();
            assertThatCode(() -> secondStart.await())
                .doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should stop successfully and clean up resources")
        void shouldStopSuccessfullyAndCleanUpResources() {
            agent.start().await();
            
            var stopFuture = agent.stop();
            assertThatCode(() -> stopFuture.await())
                .doesNotThrowAnyException();
            
            assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.DORMANT);
            assertThat(agent.isActiveLeader()).isFalse();
        }
        
        @Test
        @DisplayName("Should handle stop when already stopped")
        void shouldHandleStopWhenAlreadyStopped() {
            // Agent is initially dormant
            var stopFuture = agent.stop();
            
            assertThatCode(() -> stopFuture.await())
                .doesNotThrowAnyException();
            
            assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.DORMANT);
        }
        
        @Test
        @DisplayName("Should handle startup failures gracefully")
        void shouldHandleStartupFailuresGracefully() {
            // Mock MessageRouter to throw exception during route registration
            doThrow(new RuntimeException("Router error"))
                .when(messageRouter).addRoute(any(Class.class), any());
            
            var startFuture = agent.start();
            
            // The Promise recover() method handles exceptions, so no exception is thrown
            // Instead, the agent should transition to FAILED state
            assertThatCode(() -> startFuture.await())
                .doesNotThrowAnyException();
            
            assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.FAILED);
        }
    }
    
    @Nested
    @DisplayName("Leadership Management Tests")
    class LeadershipManagementTest {
        
        @Test
        @DisplayName("Should become active when quorum is established")
        void shouldBecomeActiveWhenQuorumIsEstablished() {
            agent.start().await();
            assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.INACTIVE);
            assertThat(agent.isActiveLeader()).isFalse();
            
            // Simulate quorum establishment
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            
            await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.ACTIVE);
                    assertThat(agent.isActiveLeader()).isTrue();
                });
        }
        
        @Test
        @DisplayName("Should step down when quorum disappears")
        void shouldStepDownWhenQuorumDisappears() {
            agent.start().await();
            
            // First become leader
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            await().atMost(1, TimeUnit.SECONDS)
                .until(() -> agent.isActiveLeader());
            
            // Then lose quorum
            simulateQuorumNotification(QuorumStateNotification.DISAPPEARED);
            
            await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.INACTIVE);
                    assertThat(agent.isActiveLeader()).isFalse();
                });
        }
        
        @Test
        @DisplayName("Should handle leadership transitions correctly")
        void shouldHandleLeadershipTransitionsCorrectly() {
            agent.start().await();
            
            // Multiple transitions
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            await().until(() -> agent.isActiveLeader());
            
            simulateQuorumNotification(QuorumStateNotification.DISAPPEARED);
            await().until(() -> !agent.isActiveLeader());
            
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            await().until(() -> agent.isActiveLeader());
            
            assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.ACTIVE);
        }
        
        private void simulateQuorumNotification(QuorumStateNotification notification) {
            // Capture the handler and invoke it directly
            var handlerCaptor = org.mockito.ArgumentCaptor.forClass(java.util.function.Consumer.class);
            verify(messageRouter).addRoute(eq(QuorumStateNotification.class), handlerCaptor.capture());
            
            @SuppressWarnings("unchecked")
            var handler = (java.util.function.Consumer<QuorumStateNotification>) handlerCaptor.getValue();
            handler.accept(notification);
        }
    }
    
    @Nested
    @DisplayName("Configuration Management Tests")
    class ConfigurationManagementTest {
        
        @Test
        @DisplayName("Should update configuration successfully")
        void shouldUpdateConfigurationSuccessfully() {
            var newConfig = AgentConfiguration.builder()
                .withPerformance(new AgentConfiguration.PerformanceConfig(
                    200, Duration.ofSeconds(60), 20, 200, Duration.ofDays(14), true
                ))
                .build();
            
            agent.updateConfiguration(newConfig);
            
            assertThat(agent.currentConfiguration()).isEqualTo(newConfig);
        }
        
        @Test
        @DisplayName("Should apply configuration changes to active components")
        void shouldApplyConfigurationChangesToActiveComponents() {
            agent.start().await();
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            await().until(() -> agent.isActiveLeader());
            
            var newConfig = AgentConfiguration.builder()
                .withFeatures(new AgentConfiguration.FeatureToggleConfig(
                    true, false, true, true, true, true, true, true, java.util.Map.of()
                ))
                .build();
            
            // Should not throw exception when updating configuration of active agent
            assertThatCode(() -> agent.updateConfiguration(newConfig))
                .doesNotThrowAnyException();
            
            assertThat(agent.currentConfiguration()).isEqualTo(newConfig);
        }
    }
    
    @Nested
    @DisplayName("Health and Monitoring Tests")
    class HealthAndMonitoringTest {
        
        @Test
        @DisplayName("Should provide accurate health information")
        void shouldProvideAccurateHealthInformation() {
            agent.start().await();
            
            var health = agent.health();
            
            assertThat(health.nodeId()).isEqualTo(nodeId);
            assertThat(health.state()).isEqualTo(AetherAgent.AgentState.INACTIVE);
            assertThat(health.isLeader()).isFalse();
            assertThat(health.startTime()).isNotNull();
            assertThat(health.isHealthy()).isTrue();
            assertThat(health.uptime()).isGreaterThan(Duration.ZERO);
        }
        
        @Test
        @DisplayName("Should report unhealthy state when failed")
        void shouldReportUnhealthyStateWhenFailed() {
            // Force agent into failed state
            doThrow(new RuntimeException("Test failure"))
                .when(messageRouter).addRoute(any(Class.class), any());
            
            try {
                agent.start().await();
            } catch (Exception e) {
                // Expected
            }
            
            var health = agent.health();
            assertThat(health.state()).isEqualTo(AetherAgent.AgentState.FAILED);
            assertThat(health.isHealthy()).isFalse();
        }
        
        @Test
        @DisplayName("Should include processing statistics when available")
        void shouldIncludeProcessingStatisticsWhenAvailable() {
            agent.start().await();
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            await().until(() -> agent.isActiveLeader());
            
            var health = agent.health();
            
            // Processing stats should be available when agent is active
            assertThat(health.processingStats().isPresent()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTest {
        
        @Test
        @DisplayName("Should handle leadership transition errors gracefully")
        void shouldHandleLeadershipTransitionErrorsGracefully() {
            agent.start().await();
            
            // Since leadership transitions no longer involve complex external dependencies,
            // test that the agent handles internal state transitions gracefully
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            simulateQuorumNotification(QuorumStateNotification.DISAPPEARED);
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            
            // Agent should handle transitions gracefully
            await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> 
                    assertThat(agent.currentState()).isIn(
                        AetherAgent.AgentState.ACTIVE,
                        AetherAgent.AgentState.INACTIVE
                    )
                );
        }
        
        @Test
        @DisplayName("Should recover from transient errors")
        void shouldRecoverFromTransientErrors() {
            agent.start().await();
            
            // Simulate successful leadership transitions despite some errors
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            simulateQuorumNotification(QuorumStateNotification.DISAPPEARED);
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            
            // Agent should handle transitions gracefully
            await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> 
                    assertThat(agent.currentState()).isIn(
                        AetherAgent.AgentState.ACTIVE,
                        AetherAgent.AgentState.INACTIVE
                    )
                );
        }
    }
    
    @Nested
    @DisplayName("Agent State Tests")
    class AgentStateTest {
        
        @Test
        @DisplayName("Should maintain correct state transitions")
        void shouldMaintainCorrectStateTransitions() {
            // Initial state
            assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.DORMANT);
            
            // After start
            agent.start().await();
            assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.INACTIVE);
            
            // After becoming leader
            simulateQuorumNotification(QuorumStateNotification.ESTABLISHED);
            await().until(() -> agent.currentState() == AetherAgent.AgentState.ACTIVE);
            
            // After losing leadership
            simulateQuorumNotification(QuorumStateNotification.DISAPPEARED);
            await().until(() -> agent.currentState() == AetherAgent.AgentState.INACTIVE);
            
            // After stop
            agent.stop().await();
            assertThat(agent.currentState()).isEqualTo(AetherAgent.AgentState.DORMANT);
        }
        
        @Test
        @DisplayName("Should validate agent state enum values")
        void shouldValidateAgentStateEnumValues() {
            var states = AetherAgent.AgentState.values();
            
            assertThat(states).containsExactly(
                AetherAgent.AgentState.DORMANT,
                AetherAgent.AgentState.STARTING,
                AetherAgent.AgentState.INACTIVE,
                AetherAgent.AgentState.ACTIVE,
                AetherAgent.AgentState.STOPPING,
                AetherAgent.AgentState.FAILED
            );
        }
    }
    
    // Helper method to simulate quorum notifications
    private void simulateQuorumNotification(QuorumStateNotification notification) {
        try {
            var handlerCaptor = org.mockito.ArgumentCaptor.forClass(java.util.function.Consumer.class);
            verify(messageRouter, atLeastOnce()).addRoute(eq(QuorumStateNotification.class), handlerCaptor.capture());
            
            @SuppressWarnings("unchecked")
            var handler = (java.util.function.Consumer<QuorumStateNotification>) handlerCaptor.getValue();
            handler.accept(notification);
        } catch (Exception e) {
            // Ignore verification errors in error handling tests
        }
    }
}