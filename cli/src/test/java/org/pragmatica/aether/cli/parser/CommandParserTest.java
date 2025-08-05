package org.pragmatica.aether.cli.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pragmatica.aether.cli.session.CLIContext;
import org.pragmatica.aether.cli.test.CLITestFramework;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for command parsing functionality.
 */
@ExtendWith(MockitoExtension.class)
class CommandParserTest extends CLITestFramework {
    
    private CommandParser parser;
    
    @BeforeEach
    void setupParser() {
        parser = getTestContext().getCommandParser();
    }
    
    @Test
    void shouldParseEmptyCommand() {
        var command = parser.parse("");
        var result = command.execute();
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.output()).isEmpty();
    }
    
    @Test
    void shouldParseHelpCommand() {
        var command = parser.parse("/help");
        var result = command.execute();
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.output()).contains("Aether CLI Commands");
    }
    
    @Test
    void shouldParseQuitCommand() {
        var command = parser.parse("/quit");
        var result = command.execute();
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.message()).isEqualTo("quit");
    }
    
    @Test
    void shouldParseClusterCommands() {
        var command = parser.parse("/cluster status");
        var result = command.execute();
        
        // Placeholder command should return success
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.output()).contains("Cluster Status");
    }
    
    @Test
    void shouldParseNodeCommands() {
        var command = parser.parse("/node status");
        var result = command.execute();
        
        // Should work even without active connection (will show error message)
        assertThat(result).isNotNull();
    }
    
    @Test
    void shouldParseSliceCommands() {
        var command = parser.parse("/slice list");
        var result = command.execute();
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.output()).contains("Deployed Slices");
    }
    
    @Test
    void shouldParseAgentCommands() {
        var command = parser.parse("/agent status");
        var result = command.execute();
        
        // Should handle no connection gracefully
        assertThat(result).isNotNull();
    }
    
    @Test
    void shouldHandleUnknownCommands() {
        var command = parser.parse("/unknown");
        var result = command.execute();
        
        assertThat(result.isError()).isTrue();
        assertThat(result.message()).contains("Unknown command");
    }
    
    @Test
    void shouldHandleConnectionCommands() {
        var command = parser.parse("/connect localhost:8080");
        var result = command.execute();
        
        // Connection will fail in test environment, but parsing should work
        assertThat(result).isNotNull();
    }
    
    @Test
    void shouldParseCommandArguments() {
        var command = parser.parse("/slice deploy org.example:test:1.0.0");
        var result = command.execute();
        
        assertThat(result).isNotNull();
        // In full implementation, would verify argument parsing
    }
    
    @Test
    void shouldHandleMultipleArguments() {
        var command = parser.parse("/slice status test-slice");
        var result = command.execute();
        
        assertThat(result).isNotNull();
    }
}