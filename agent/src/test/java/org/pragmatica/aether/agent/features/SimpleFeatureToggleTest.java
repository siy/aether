package org.pragmatica.aether.agent.features;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;

/**
 * Simple feature toggle tests to demonstrate the core functionality.
 */
@DisplayName("Simple Feature Toggle Tests")
class SimpleFeatureToggleTest {
    
    private SimpleFeatureToggle featureToggle;
    
    @BeforeEach
    void setUp() {
        featureToggle = new SimpleFeatureToggle();
    }
    
    @Test
    @DisplayName("Test basic feature toggle functionality")
    void testBasicFeatureToggle() {
        var testToggle = new SimpleFeatureToggle();
        var featureKey = "test.feature";
        
        // Initially should be disabled
        assertThat(testToggle.isEnabled(featureKey)).isFalse();
        
        // Enable the feature
        testToggle.setEnabled(featureKey, true);
        assertThat(testToggle.isEnabled(featureKey)).isTrue();
        
        // Disable the feature
        testToggle.setEnabled(featureKey, false);
        assertThat(testToggle.isEnabled(featureKey)).isFalse();
    }
    
    @Test
    @DisplayName("Test known features with defaults")
    void testKnownFeatures() {
        // Test some default values
        assertThat(featureToggle.isEnabled("agent.enabled")).isTrue();
        assertThat(featureToggle.isEnabled("agent.shadow_mode")).isFalse();
        assertThat(featureToggle.isEnabled("llm.local.enabled")).isTrue();
        assertThat(featureToggle.isEnabled("llm.cloud.enabled")).isFalse();
    }
    
    @Test
    @DisplayName("Test emergency mode")
    void testEmergencyMode() {
        var featureKey = "test.feature";
        
        // Enable a feature
        featureToggle.setEnabled(featureKey, true);
        assertThat(featureToggle.isEnabled(featureKey)).isTrue();
        assertThat(featureToggle.isEmergencyMode()).isFalse();
        
        // Activate emergency mode
        featureToggle.emergencyDisableAll();
        
        // All features should be disabled
        assertThat(featureToggle.isEnabled(featureKey)).isFalse();
        assertThat(featureToggle.isEnabled("agent.enabled")).isFalse();
        assertThat(featureToggle.isEmergencyMode()).isTrue();
        
        // Restore from emergency
        featureToggle.restoreFromEmergency();
        
        // Features should be restored
        assertThat(featureToggle.isEnabled(featureKey)).isTrue();
        assertThat(featureToggle.isEnabled("agent.enabled")).isTrue();
        assertThat(featureToggle.isEmergencyMode()).isFalse();
    }
    
    @Test
    @DisplayName("Test multiple features")
    void testMultipleFeatures() {
        featureToggle.setEnabled("feature1", true);
        featureToggle.setEnabled("feature2", false);
        featureToggle.setEnabled("feature3", true);
        
        assertThat(featureToggle.isEnabled("feature1")).isTrue();
        assertThat(featureToggle.isEnabled("feature2")).isFalse();
        assertThat(featureToggle.isEnabled("feature3")).isTrue();
    }
    
    /**
     * Simple feature toggle implementation for testing.
     */
    public static class SimpleFeatureToggle {
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
}