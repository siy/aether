package org.pragmatica.aether.agent.features;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.pragmatica.lang.Option;

/**
 * Simple feature toggle implementation for testing and demonstration.
 */
public class SimpleFeatureToggle implements FeatureToggle {
    private final Map<String, Boolean> features = new ConcurrentHashMap<>();
    private volatile boolean emergencyMode = false;
    
    public SimpleFeatureToggle() {
        // Initialize with default values
        initializeDefaults();
    }
    
    @Override
    public boolean isEnabled(String featureKey, EvaluationContext context) {
        if (emergencyMode) {
            return false;
        }
        
        return features.getOrDefault(featureKey, false);
    }
    
    public boolean isEnabled(String featureKey) {
        return isEnabled(featureKey, EvaluationContext.empty());
    }
    
    public void setEnabled(String featureKey, boolean enabled) {
        features.put(featureKey, enabled);
    }
    
    @Override
    public void updateToggle(ToggleConfig config) {
        features.put(config.featureKey(), config.enabled());
    }
    
    @Override
    public Option<ToggleConfig> toggleConfig(String featureKey) {
        return Option.some(ToggleConfig.builder(featureKey)
            .enabled(features.getOrDefault(featureKey, false))
            .build());
    }
    
    @Override
    public Map<String, ToggleConfig> allToggles() {
        return features.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> ToggleConfig.builder(entry.getKey())
                    .enabled(entry.getValue())
                    .build()
            ));
    }
    
    @Override
    public void removeToggle(String featureKey) {
        features.remove(featureKey);
    }
    
    @Override
    public void addToggleListener(String featureKey, ToggleListener listener) {
        // Simple implementation - no listeners for now
    }
    
    @Override
    public void removeToggleListener(String featureKey, ToggleListener listener) {
        // Simple implementation - no listeners for now
    }
    
    @Override
    public void emergencyDisableAll() {
        emergencyMode = true;
    }
    
    @Override
    public void restoreFromEmergency() {
        emergencyMode = false;
    }
    
    @Override
    public boolean isEmergencyMode() {
        return emergencyMode;
    }
    
    private void initializeDefaults() {
        // Set defaults for known features
        features.put("agent.enabled", true);
        features.put("agent.shadow_mode", false);
        features.put("agent.recommendations.enabled", true);
        features.put("agent.cli.natural_language", true);
        
        features.put("llm.local.enabled", true);
        features.put("llm.cloud.enabled", false);
        features.put("llm.fallback.enabled", true);
        features.put("llm.cost_limits.enabled", true);
        
        features.put("agent.learning.enabled", false);
        features.put("agent.autonomy.enabled", false);
        features.put("agent.predictive.enabled", false);
        features.put("agent.cross_cluster.enabled", false);
    }
}