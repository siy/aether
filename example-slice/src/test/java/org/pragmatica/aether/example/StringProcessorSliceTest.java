package org.pragmatica.aether.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringProcessorSliceTest {

    @Test
    void slice_can_be_started() {
        var slice = new StringProcessorSlice();
        
        slice.start()
             .onSuccess(activeSlice -> {
                 assertThat(activeSlice).isNotNull();
                 assertThat(activeSlice).isInstanceOf(StringProcessorEntryPoint.class);
             })
             .onFailureRun(Assertions::fail);
    }

    @Test
    void active_slice_can_be_stopped() {
        var slice = new StringProcessorSlice();
        
        slice.start()
             .flatMap(activeSlice -> activeSlice.stop())
             .onSuccessRun(() -> {
                 // Stop completed successfully
                 assertThat(true).isTrue();
             })
             .onFailureRun(Assertions::fail);
    }

    @Test
    void toLowerCase_converts_string_to_lowercase() {
        var slice = new StringProcessorSlice();
        
        slice.start()
             .onSuccess(activeSlice -> {
                 var processor = (StringProcessorEntryPoint) activeSlice;
                 
                 assertThat(processor.toLowerCase("HELLO WORLD")).isEqualTo("hello world");
                 assertThat(processor.toLowerCase("Mixed Case")).isEqualTo("mixed case");
                 assertThat(processor.toLowerCase("already lowercase")).isEqualTo("already lowercase");
             })
             .onFailureRun(Assertions::fail);
    }

    @Test
    void toLowerCase_handles_null_input() {
        var slice = new StringProcessorSlice();
        
        slice.start()
             .onSuccess(activeSlice -> {
                 var processor = (StringProcessorEntryPoint) activeSlice;
                 
                 assertThat(processor.toLowerCase(null)).isNull();
             })
             .onFailureRun(Assertions::fail);
    }

    @Test
    void toLowerCase_handles_empty_string() {
        var slice = new StringProcessorSlice();
        
        slice.start()
             .onSuccess(activeSlice -> {
                 var processor = (StringProcessorEntryPoint) activeSlice;
                 
                 assertThat(processor.toLowerCase("")).isEqualTo("");
             })
             .onFailureRun(Assertions::fail);
    }

    @Test
    void entry_point_definition_is_correct() {
        var entryPoint = StringProcessorEntryPoint.ENTRY_POINT;
        
        assertThat(entryPoint.id().toString()).isEqualTo("toLowerCase");
        assertThat(entryPoint.parameterTypes()).hasSize(1);
        // TypeToken verification - just check it's not null and size is correct
        assertThat(entryPoint.parameterTypes().get(0)).isNotNull();
    }
}