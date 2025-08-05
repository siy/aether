package org.pragmatica.aether.agent.features;

import org.pragmatica.lang.Option;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simplified feature toggle system for the Aether Agent.
 * <p>
 * Provides runtime configuration management without requiring restarts,
 * enabling gradual rollouts and emergency kill switches.
 * Integrates with the consensus system to ensure consistent feature states
 * across all cluster nodes.
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

        public String key() {
            return key;
        }

        public String description() {
            return description;
        }

        public boolean defaultValue() {
            return defaultValue;
        }

        /**
         * Lookup map for converting string keys to enum values.
         */
        private static final Map<String, KnownFeature> BY_KEY =
                Stream.of(values())
                      .collect(Collectors.toMap(KnownFeature::key, Function.identity()));

        /**
         * Convert string key to enum value.
         * Used for configuration loading and external integrations.
         *
         * @param key The string key to look up
         *
         * @return Option containing the feature if found, empty otherwise
         */
        public static Option<KnownFeature> fromKey(String key) {
            return Option.option(BY_KEY.get(key));
        }
    }

    /**
     * Evaluates whether a feature is enabled.
     * Primary API - uses enum for type safety.
     *
     * @param feature The feature to evaluate
     *
     * @return true if the feature is enabled
     */
    boolean isEnabled(KnownFeature feature);

    /**
     * Sets a feature toggle state.
     * Simple API for basic enable/disable operations.
     *
     * @param feature The feature to update
     * @param enabled Whether the feature should be enabled
     */
    void setEnabled(KnownFeature feature, boolean enabled);

    /**
     * Loads configuration from external sources (JSON, properties, etc.).
     * Converts string keys to enum values automatically.
     *
     * @param config Map of feature keys to enabled states
     */
    void loadConfig(Map<String, Boolean> config);

    /**
     * Exports current configuration as string map.
     * Useful for saving to files or external systems.
     *
     * @return Map of feature keys to their current states
     */
    Map<String, Boolean> exportConfig();

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