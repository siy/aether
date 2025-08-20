package org.pragmatica.aether.example;

import org.pragmatica.aether.slice.EntryPoint;
import org.pragmatica.aether.slice.EntryPointId;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;

/**
 * Entry point for string processing operations.
 * This interface defines the available methods that can be called on this slice.
 */
public interface StringProcessorEntryPoint {
    
    /**
     * Converts input string to lowercase.
     * 
     * @param input the string to convert
     * @return the input string converted to lowercase
     */
    String toLowerCase(String input);
    
    /**
     * Entry point definition for the toLowerCase method.
     */
    EntryPoint ENTRY_POINT = EntryPoint.entry(
        EntryPointId.entryPointId("toLowerCase").unwrap(),
        List.of(TypeToken.of(String.class))
    );
}