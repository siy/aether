package org.pragmatica.aether.cli.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for session history functionality.
 */
@ExtendWith(MockitoExtension.class)
class SessionHistoryTest {
    
    private SessionHistory history;
    
    @BeforeEach
    void setupHistory() {
        history = new SessionHistory();
    }
    
    @Test
    void shouldAddCommandToHistory() {
        history.addCommand("/cluster status");
        
        var entries = history.getEntries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).commandLine()).isEqualTo("/cluster status");
        assertThat(entries.get(0).sequenceNumber()).isEqualTo(1);
    }
    
    @Test
    void shouldAddMultipleCommands() {
        history.addCommand("/cluster status");
        history.addCommand("/node info");
        history.addCommand("/slice list");
        
        var entries = history.getEntries();
        assertThat(entries).hasSize(3);
        assertThat(history.getCommandCount()).isEqualTo(3);
    }
    
    @Test
    void shouldAddResultToLastCommand() {
        history.addCommand("/cluster status");
        var result = CLIContext.CommandResult.success("Cluster is healthy");
        history.addResult(result);
        
        var entries = history.getEntries();
        assertThat(entries.get(0).result()).isEqualTo(result);
        assertThat(entries.get(0).wasSuccessful()).isTrue();
    }
    
    @Test
    void shouldGetLastCommands() {
        for (int i = 1; i <= 5; i++) {
            history.addCommand("/command" + i);
        }
        
        var lastThree = history.getLastCommands(3);
        assertThat(lastThree).hasSize(3);
        assertThat(lastThree.get(0).commandLine()).isEqualTo("/command3");
        assertThat(lastThree.get(2).commandLine()).isEqualTo("/command5");
    }
    
    @Test
    void shouldFindCommandBySequence() {
        history.addCommand("/cluster status");
        history.addCommand("/node info");
        
        var command = history.getCommandBySequence(1);
        assertThat(command).isNotNull();
        assertThat(command.commandLine()).isEqualTo("/cluster status");
        
        var notFound = history.getCommandBySequence(10);
        assertThat(notFound).isNull();
    }
    
    @Test
    void shouldSearchCommands() {
        history.addCommand("/cluster status");
        history.addCommand("/node status");
        history.addCommand("/slice list");
        
        var statusCommands = history.searchCommands("status");
        assertThat(statusCommands).hasSize(2);
        assertThat(statusCommands.get(0).commandLine()).contains("status");
        assertThat(statusCommands.get(1).commandLine()).contains("status");
    }
    
    @Test
    void shouldCalculateSessionStats() {
        history.addCommand("/cluster status");
        history.addResult(CLIContext.CommandResult.success("OK"));
        
        history.addCommand("/invalid command");
        history.addResult(CLIContext.CommandResult.error("Command failed"));
        
        history.addCommand("/slice list");
        history.addResult(CLIContext.CommandResult.warning("No slices", "Empty list"));
        
        var stats = history.getStats();
        assertThat(stats.totalCommands()).isEqualTo(3);
        assertThat(stats.successfulCommands()).isEqualTo(1);
        assertThat(stats.failedCommands()).isEqualTo(1);
        assertThat(stats.warningCommands()).isEqualTo(1);
        assertThat(stats.getSuccessRate()).isCloseTo(33.33, within(0.01));
    }
    
    @Test
    void shouldClearHistory() {
        history.addCommand("/cluster status");
        history.addCommand("/node info");
        
        assertThat(history.getCommandCount()).isEqualTo(2);
        
        history.clear();
        
        assertThat(history.getCommandCount()).isEqualTo(0);
        assertThat(history.getEntries()).isEmpty();
    }
    
    @Test
    void shouldRespectHistoryLimit() {
        var limitedHistory = new SessionHistory(3);
        
        for (int i = 1; i <= 5; i++) {
            limitedHistory.addCommand("/command" + i);
        }
        
        var entries = limitedHistory.getEntries();
        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).commandLine()).isEqualTo("/command3");
        assertThat(entries.get(2).commandLine()).isEqualTo("/command5");
    }
    
    private static org.assertj.core.data.Offset<Double> within(double offset) {
        return org.assertj.core.data.Offset.offset(offset);
    }
}