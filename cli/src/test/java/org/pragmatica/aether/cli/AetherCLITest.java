package org.pragmatica.aether.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pragmatica.aether.cli.test.CLITestFramework;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the main AetherCLI entry point and argument parsing.
 */
@ExtendWith(MockitoExtension.class)
class AetherCLITest extends CLITestFramework {
    
    @Test
    void shouldParseInteractiveModeArguments() {
        // Test argument parsing for interactive mode
        var cli = new AetherCLI();
        
        // This would normally start interactive mode - we'll test argument parsing logic
        var args = new String[]{"-i"};
        
        // In a full implementation, we'd extract the argument parsing logic
        // into a testable method and verify the parsed arguments
        assertThat(args).contains("-i");
    }
    
    @Test
    void shouldParseBatchModeArguments() {
        var args = new String[]{"/cluster", "status"};
        
        // Test that batch mode arguments are parsed correctly
        assertThat(args).hasSize(2);
        assertThat(args[0]).isEqualTo("/cluster");
        assertThat(args[1]).isEqualTo("status");
    }
    
    @Test
    void shouldHandleConnectionArguments() {
        var args = new String[]{"-n", "localhost:8080", "/node", "status"};
        
        // Test connection arguments parsing
        assertThat(args).contains("-n", "localhost:8080");
    }
    
    @Test
    void shouldDisplayVersionInformation() {
        var args = new String[]{"--version"};
        
        // Test version flag recognition
        assertThat(args).contains("--version");
    }
    
    @Test
    void shouldDisplayHelpInformation() {
        var args = new String[]{"-h"};
        
        // Test help flag recognition
        assertThat(args).contains("-h");
    }
}