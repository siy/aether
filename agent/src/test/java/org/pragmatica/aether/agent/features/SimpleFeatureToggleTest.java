package org.pragmatica.aether.agent.features;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.pragmatica.aether.agent.features.FeatureToggle.KnownFeature.*;

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
        // Test with enum-based API
        assertThat(featureToggle.isEnabled(AGENT_ENABLED)).isTrue();
        
        // Enable/disable the feature
        featureToggle.updateToggle(AGENT_ENABLED, false);
        assertThat(featureToggle.isEnabled(AGENT_ENABLED)).isFalse();
        
        // Re-enable the feature
        featureToggle.updateToggle(AGENT_ENABLED, true);
        assertThat(featureToggle.isEnabled(AGENT_ENABLED)).isTrue();
    }
    
    @Test
    @DisplayName("Test known features with defaults")
    void testKnownFeatures() {
        // Test some default values
        assertThat(featureToggle.isEnabled(AGENT_ENABLED)).isTrue();
        assertThat(featureToggle.isEnabled(AGENT_SHADOW_MODE)).isFalse();
        assertThat(featureToggle.isEnabled(LLM_LOCAL_ENABLED)).isTrue();
        assertThat(featureToggle.isEnabled(LLM_CLOUD_ENABLED)).isFalse();
    }
    
    @Test
    @DisplayName("Test emergency mode")
    void testEmergencyMode() {
        // Enable a feature and verify normal state
        featureToggle.updateToggle(AGENT_RECOMMENDATIONS_ENABLED, true);
        assertThat(featureToggle.isEnabled(AGENT_RECOMMENDATIONS_ENABLED)).isTrue();
        assertThat(featureToggle.isEmergencyMode()).isFalse();
        
        // Activate emergency mode
        featureToggle.emergencyDisableAll();
        
        // All features should be disabled
        assertThat(featureToggle.isEnabled(AGENT_RECOMMENDATIONS_ENABLED)).isFalse();
        assertThat(featureToggle.isEnabled(AGENT_ENABLED)).isFalse();
        assertThat(featureToggle.isEmergencyMode()).isTrue();
        
        // Restore from emergency
        featureToggle.restoreFromEmergency();
        
        // Features should be restored
        assertThat(featureToggle.isEnabled(AGENT_RECOMMENDATIONS_ENABLED)).isTrue();
        assertThat(featureToggle.isEnabled(AGENT_ENABLED)).isTrue();
        assertThat(featureToggle.isEmergencyMode()).isFalse();
    }
    
    @Test
    @DisplayName("Test multiple features")
    void testMultipleFeatures() {
        featureToggle.updateToggle(AGENT_ENABLED, true);
        featureToggle.updateToggle(AGENT_SHADOW_MODE, false);
        featureToggle.updateToggle(LLM_LOCAL_ENABLED, true);
        
        assertThat(featureToggle.isEnabled(AGENT_ENABLED)).isTrue();
        assertThat(featureToggle.isEnabled(AGENT_SHADOW_MODE)).isFalse();
        assertThat(featureToggle.isEnabled(LLM_LOCAL_ENABLED)).isTrue();
    }
    
    @Test
    @DisplayName("Test natural syntax with in() and notIn() methods")
    void testNaturalSyntax() {
        // Test the natural FEATURE.in(toggle) syntax
        assertThat(AGENT_ENABLED.in(featureToggle)).isTrue();
        assertThat(AGENT_SHADOW_MODE.in(featureToggle)).isFalse();
        
        // Test the natural FEATURE.notIn(toggle) syntax
        assertThat(AGENT_ENABLED.notIn(featureToggle)).isFalse();
        assertThat(AGENT_SHADOW_MODE.notIn(featureToggle)).isTrue();
        
        // Change feature state and test again
        featureToggle.updateToggle(AGENT_SHADOW_MODE, true);
        assertThat(AGENT_SHADOW_MODE.in(featureToggle)).isTrue();
        assertThat(AGENT_SHADOW_MODE.notIn(featureToggle)).isFalse();
    }
    
    @Test
    @DisplayName("Test config loading and exporting")
    void testConfigLoadingExporting() {
        // Modify some features
        featureToggle.updateToggle(AGENT_SHADOW_MODE, true);
        featureToggle.updateToggle(LLM_CLOUD_ENABLED, true);
        
        // Export config
        var config = featureToggle.exportConfig();
        assertThat(config).containsEntry(AGENT_SHADOW_MODE.key(), true);
        assertThat(config).containsEntry(LLM_CLOUD_ENABLED.key(), true);
        
        // Create new toggle and load config
        var newToggle = new SimpleFeatureToggle();
        newToggle.loadFromConfig(config);
        
        // Verify the state was loaded
        assertThat(newToggle.isEnabled(AGENT_SHADOW_MODE)).isTrue();
        assertThat(newToggle.isEnabled(LLM_CLOUD_ENABLED)).isTrue();
    }
    
    @Test
    @DisplayName("Test unknown feature key handling")
    void testUnknownFeatureKeys() {
        // Create config with unknown key
        var config = Map.of(
            "agent.enabled", true,
            "unknown.feature", true,
            "llm.local.enabled", false
        );
        
        // Should load known features and ignore unknown ones
        featureToggle.loadFromConfig(config);
        
        assertThat(featureToggle.isEnabled(AGENT_ENABLED)).isTrue();
        assertThat(featureToggle.isEnabled(LLM_LOCAL_ENABLED)).isFalse();
    }
    
    @Test
    @DisplayName("Test Option-based fromKey method")
    void testFromKeyReturnsOption() {
        // Test with valid key - no extraction needed
        FeatureToggle.KnownFeature.fromKey("agent.enabled")
            .onEmpty(() -> fail("Expected feature to be found"))
            .onPresent(feature -> assertThat(feature).isEqualTo(AGENT_ENABLED));
        
        // Test with invalid key - should not call onPresent
        FeatureToggle.KnownFeature.fromKey("non.existent.feature")
            .onPresentRun(() -> fail("Should not find non-existent feature"));
        
        // Test Option chaining with valid key
        FeatureToggle.KnownFeature.fromKey("llm.local.enabled")
            .map(FeatureToggle.KnownFeature::key)
            .onEmpty(() -> fail("Expected to find llm.local.enabled"))
            .onPresent(key -> assertThat(key).isEqualTo("llm.local.enabled"));
        
        // Test Option chaining with invalid key - using or() for fallback
        var defaultResult = FeatureToggle.KnownFeature.fromKey("unknown")
            .map(FeatureToggle.KnownFeature::key)
            .or(() -> "default");
        assertThat(defaultResult).isEqualTo("default");
    }
}