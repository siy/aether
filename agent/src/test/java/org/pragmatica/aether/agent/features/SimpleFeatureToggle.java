package org.pragmatica.aether.agent.features;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple feature toggle implementation for testing and demonstration.
 */
public class SimpleFeatureToggle {
    private final Map<String, Boolean> features = new ConcurrentHashMap<>();
    private volatile boolean emergencyMode = false;
    
    public SimpleFeatureToggle() {
        // Initialize with default values
        initializeDefaults();
    }
    
    public boolean isEnabled(String featureKey) {
        if (emergencyMode) {
            return false;
        }
        
        return features.getOrDefault(featureKey, false);
    }
    
    public void setEnabled(String featureKey, boolean enabled) {
        features.put(featureKey, enabled);
    }
    
    public void emergencyDisableAll() {
        emergencyMode = true;
    }
    
    public void restoreFromEmergency() {
        emergencyMode = false;
    }
    
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