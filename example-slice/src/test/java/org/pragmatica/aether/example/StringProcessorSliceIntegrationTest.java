package org.pragmatica.aether.example;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class StringProcessorSliceIntegrationTest {

    @Test
    void slice_can_be_discovered_via_service_loader() throws Exception {
        // This test demonstrates that the slice can be discovered via ServiceLoader
        // which is the mechanism used by SliceStore
        
        var jarPath = System.getProperty("user.dir") + "/target/example-slice-0.1.0.jar";
        var jarUrl = new java.io.File(jarPath).toURI().toURL();
        
        try (var classLoader = new java.net.URLClassLoader(new URL[]{jarUrl}, 
                                                            getClass().getClassLoader())) {
            var serviceLoader = ServiceLoader.load(
                org.pragmatica.aether.slice.Slice.class, 
                classLoader
            );
            
            var slices = serviceLoader.stream().toList();
            
            assertThat(slices).hasSize(1);
            
            var sliceProvider = slices.get(0);
            var slice = sliceProvider.get();
            
            assertThat(slice).isInstanceOf(StringProcessorSlice.class);
            
            // Test the complete lifecycle
            slice.start()
                 .onSuccess(activeSlice -> {
                     assertThat(activeSlice).isInstanceOf(StringProcessorEntryPoint.class);
                     
                     var processor = (StringProcessorEntryPoint) activeSlice;
                     var result = processor.toLowerCase("INTEGRATION TEST");
                     
                     assertThat(result).isEqualTo("integration test");
                     
                     // Test stop
                     activeSlice.stop()
                              .onSuccess(unit -> assertThat(unit).isNotNull())
                              .onFailure(cause -> {
                                  throw new AssertionError("Stop should not fail: " + cause.message());
                              });
                 })
                 .onFailure(cause -> {
                     throw new AssertionError("Start should not fail: " + cause.message());
                 });
        }
    }
}