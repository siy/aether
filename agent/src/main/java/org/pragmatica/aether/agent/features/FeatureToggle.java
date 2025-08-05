package org.pragmatica.aether.agent.features;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Feature toggle system for the Aether Agent.
 * 
 * Provides runtime configuration management without requiring restarts,
 * enabling gradual rollouts, A/B testing, and emergency kill switches.
 * Integrates with the consensus system to ensure consistent feature states
 * across all cluster nodes.
 * 
 * The feature toggle system supports hierarchical features with inheritance,
 * user segmentation for targeted rollouts, and percentage-based gradual
 * deployments. All toggle states are stored in consensus to maintain
 * consistency across the distributed system.
 */
public interface FeatureToggle {
    
    /**
     * Predefined feature flags for the Aether Agent system.
     * Following the specification from the design document.
     */
    enum KnownFeature {
        // Core Features
        AGENT_ENABLED("agent.enabled", "Master switch for entire agent", true),
        AGENT_SHADOW_MODE("agent.shadow_mode", "Run without user visibility", false),
        AGENT_RECOMMENDATIONS_ENABLED("agent.recommendations.enabled", "Enable recommendation generation", true),
        AGENT_CLI_NATURAL_LANGUAGE("agent.cli.natural_language", "Enable NLP in CLI", true),
        
        // LLM Features  
        LLM_LOCAL_ENABLED("llm.local.enabled", "Enable local Qwen3 models", true),
        LLM_CLOUD_ENABLED("llm.cloud.enabled", "Enable cloud providers", false),
        LLM_FALLBACK_ENABLED("llm.fallback.enabled", "Enable automatic fallback", true),
        LLM_COST_LIMITS_ENABLED("llm.cost_limits.enabled", "Enable cost circuit breakers", true),
        
        // Advanced Features
        AGENT_LEARNING_ENABLED("agent.learning.enabled", "Enable pattern learning", false),
        AGENT_AUTONOMY_ENABLED("agent.autonomy.enabled", "Enable autonomous actions", false),
        AGENT_PREDICTIVE_ENABLED("agent.predictive.enabled", "Enable predictive analytics", false),
        AGENT_CROSS_CLUSTER_ENABLED("agent.cross_cluster.enabled", "Enable cross-cluster insights", false);
        
        private final String key;
        private final String description;
        private final boolean defaultValue;
        
        KnownFeature(String key, String description, boolean defaultValue) {
            this.key = key;
            this.description = description;
            this.defaultValue = defaultValue;
        }
        
        public String getKey() { return key; }
        public String getDescription() { return description; }
        public boolean getDefaultValue() { return defaultValue; }
    }
    
    /**
     * Toggle evaluation context for user segmentation and A/B testing.
     */
    record EvaluationContext(
        Optional<String> userId,
        Optional<String> nodeId,
        Optional<String> clusterId,
        Map<String, String> customAttributes
    ) {
        public static EvaluationContext empty() {
            return new EvaluationContext(Optional.empty(), Optional.empty(), Optional.empty(), Map.of());
        }
        
        public static EvaluationContext forNode(String nodeId) {
            return new EvaluationContext(Optional.empty(), Optional.of(nodeId), Optional.empty(), Map.of());
        }
        
        public static EvaluationContext forCluster(String clusterId) {
            return new EvaluationContext(Optional.empty(), Optional.empty(), Optional.of(clusterId), Map.of());
        }
        
        public EvaluationContext withAttribute(String key, String value) {
            var newAttributes = new java.util.HashMap<>(customAttributes);
            newAttributes.put(key, value);
            return new EvaluationContext(userId, nodeId, clusterId, Map.copyOf(newAttributes));
        }
    }
    
    /**
     * Configuration for a feature toggle.
     */
    record ToggleConfig(
        String featureKey,
        boolean enabled,
        Optional<Double> rolloutPercentage,
        Set<String> enabledUserIds,
        Set<String> enabledNodeIds,
        Set<String> enabledClusterIds,
        Map<String, String> attributeFilters,
        Optional<String> description
    ) {
        
        public static Builder builder(String featureKey) {
            return new Builder(featureKey);
        }
        
        public static class Builder {
            private final String featureKey;
            private boolean enabled = false;
            private Optional<Double> rolloutPercentage = Optional.empty();
            private Set<String> enabledUserIds = Set.of();
            private Set<String> enabledNodeIds = Set.of();
            private Set<String> enabledClusterIds = Set.of();
            private Map<String, String> attributeFilters = Map.of();
            private Optional<String> description = Optional.empty();
            
            Builder(String featureKey) {
                this.featureKey = featureKey;
            }
            
            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }
            
            public Builder rolloutPercentage(double percentage) {
                if (percentage < 0.0 || percentage > 100.0) {
                    throw new IllegalArgumentException("Rollout percentage must be between 0 and 100");
                }
                this.rolloutPercentage = Optional.of(percentage);
                return this;
            }
            
            public Builder enabledUserIds(Set<String> userIds) {
                this.enabledUserIds = Set.copyOf(userIds);
                return this;
            }
            
            public Builder enabledNodeIds(Set<String> nodeIds) {
                this.enabledNodeIds = Set.copyOf(nodeIds);
                return this;
            }
            
            public Builder enabledClusterIds(Set<String> clusterIds) {
                this.enabledClusterIds = Set.copyOf(clusterIds);
                return this;
            }
            
            public Builder attributeFilters(Map<String, String> filters) {
                this.attributeFilters = Map.copyOf(filters);
                return this;
            }
            
            public Builder description(String description) {
                this.description = Optional.of(description);
                return this;
            }
            
            public ToggleConfig build() {
                return new ToggleConfig(
                    featureKey, enabled, rolloutPercentage,
                    enabledUserIds, enabledNodeIds, enabledClusterIds,
                    attributeFilters, description
                );
            }
        }
    }
    
    /**
     * Evaluates whether a feature is enabled for the given context.
     * 
     * @param featureKey The feature key to evaluate
     * @param context The evaluation context with user/node/cluster information
     * @return true if the feature is enabled for this context
     */
    boolean isEnabled(String featureKey, EvaluationContext context);
    
    /**
     * Convenience method for evaluating known features.
     * 
     * @param feature The known feature to evaluate
     * @param context The evaluation context
     * @return true if the feature is enabled
     */
    default boolean isEnabled(KnownFeature feature, EvaluationContext context) {
        return isEnabled(feature.getKey(), context);
    }
    
    /**
     * Evaluates a feature with empty context (global setting).
     * 
     * @param featureKey The feature key to evaluate
     * @return true if the feature is globally enabled
     */
    default boolean isEnabled(String featureKey) {
        return isEnabled(featureKey, EvaluationContext.empty());
    }
    
    /**
     * Convenience method for evaluating known features globally.
     * 
     * @param feature The known feature to evaluate
     * @return true if the feature is globally enabled
     */
    default boolean isEnabled(KnownFeature feature) {
        return isEnabled(feature.getKey());
    }
    
    /**
     * Updates the configuration for a feature toggle.
     * This operation is propagated through consensus to ensure consistency.
     * 
     * @param config The new toggle configuration
     */
    void updateToggle(ToggleConfig config);
    
    /**
     * Gets the current configuration for a feature toggle.
     * 
     * @param featureKey The feature key
     * @return The current toggle configuration, or empty if not configured
     */
    Optional<ToggleConfig> getToggleConfig(String featureKey);
    
    /**
     * Gets all currently configured toggles.
     * 
     * @return Map of feature keys to their configurations
     */
    Map<String, ToggleConfig> getAllToggles();
    
    /**
     * Removes a feature toggle configuration, reverting to default behavior.
     * 
     * @param featureKey The feature key to remove
     */
    void removeToggle(String featureKey);
    
    /**
     * Registers a listener for feature toggle changes.
     * 
     * @param featureKey The feature key to listen for (or "*" for all features)
     * @param listener The listener to notify of changes
     */
    void addToggleListener(String featureKey, ToggleListener listener);
    
    /**
     * Removes a feature toggle listener.
     * 
     * @param featureKey The feature key
     * @param listener The listener to remove
     */
    void removeToggleListener(String featureKey, ToggleListener listener);
    
    /**
     * Listener interface for feature toggle changes.
     */
    @FunctionalInterface
    interface ToggleListener {
        /**
         * Called when a feature toggle configuration changes.
         * 
         * @param featureKey The feature that changed
         * @param oldConfig The previous configuration (may be null)
         * @param newConfig The new configuration (may be null if removed)
         */
        void onToggleChanged(String featureKey, ToggleConfig oldConfig, ToggleConfig newConfig);
    }
    
    /**
     * Emergency kill switch that disables all features.
     * Used for immediate system-wide feature disabling in crisis situations.
     */
    void emergencyDisableAll();
    
    /**
     * Restores normal feature toggle operation after emergency disable.
     */
    void restoreFromEmergency();
    
    /**
     * Checks if the system is in emergency mode.
     * 
     * @return true if emergency mode is active
     */
    boolean isEmergencyMode();
}