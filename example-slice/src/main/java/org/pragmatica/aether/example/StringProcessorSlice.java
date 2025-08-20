package org.pragmatica.aether.example;

import org.pragmatica.aether.slice.Slice;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

/**
 * Example slice implementation that provides simple string processing capabilities.
 * This demonstrates the complete lifecycle of a deployable slice.
 */
public class StringProcessorSlice implements Slice {
    
    @Override
    public Promise<ActiveSlice> start() {
        return Promise.success(new ActiveStringProcessorSlice());
    }
    
    /**
     * Active implementation of the string processor slice.
     * This is returned when the slice is successfully started.
     */
    public static class ActiveStringProcessorSlice implements ActiveSlice, StringProcessorEntryPoint {
        
        @Override
        public Promise<ActiveSlice> start() {
            // Already active, return this instance
            return Promise.success(this);
        }
        
        @Override
        public Promise<Unit> stop() {
            // Simple implementation - no resources to cleanup
            return Promise.success(Unit.unit());
        }
        
        @Override
        public String toLowerCase(String input) {
            if (input == null) {
                return null;
            }
            return input.toLowerCase();
        }
    }
}