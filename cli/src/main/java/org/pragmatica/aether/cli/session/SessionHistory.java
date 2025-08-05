package org.pragmatica.aether.cli.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages command history and session state for interactive CLI mode.
 * 
 * The SessionHistory provides functionality for:
 * - Command history with chronological ordering
 * - Result tracking for executed commands
 * - Session statistics and metrics
 * - History navigation support for interactive mode
 * 
 * This component supports the REPL experience by maintaining context
 * across command executions and providing history-based navigation.
 */
public class SessionHistory {
    private static final int DEFAULT_MAX_HISTORY = 1000;
    
    private final List<HistoryEntry> entries;
    private final AtomicInteger commandCounter;
    private final int maxHistorySize;
    private volatile Instant sessionStart;
    
    public SessionHistory() {
        this(DEFAULT_MAX_HISTORY);
    }
    
    public SessionHistory(int maxHistorySize) {
        this.entries = Collections.synchronizedList(new ArrayList<>());
        this.commandCounter = new AtomicInteger(0);
        this.maxHistorySize = maxHistorySize;
        this.sessionStart = Instant.now();
    }
    
    /**
     * Adds a command to the history.
     */
    public void addCommand(String commandLine) {
        var entry = new HistoryEntry(
            commandCounter.incrementAndGet(),
            commandLine,
            Instant.now(),
            null
        );
        
        addEntry(entry);
    }
    
    /**
     * Adds a command result to the most recent entry.
     */
    public void addResult(CLIContext.CommandResult result) {
        synchronized (entries) {
            if (!entries.isEmpty()) {
                var lastEntry = entries.get(entries.size() - 1);
                if (lastEntry.result == null) {
                    // Update the last entry with the result
                    var updatedEntry = new HistoryEntry(
                        lastEntry.sequenceNumber,
                        lastEntry.commandLine,
                        lastEntry.timestamp,
                        result
                    );
                    entries.set(entries.size() - 1, updatedEntry);
                }
            }
        }
    }
    
    /**
     * Gets all history entries.
     */
    public List<HistoryEntry> getEntries() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }
    
    /**
     * Gets the last N commands from history.
     */
    public List<HistoryEntry> getLastCommands(int count) {
        synchronized (entries) {
            int size = entries.size();
            int fromIndex = Math.max(0, size - count);
            return new ArrayList<>(entries.subList(fromIndex, size));
        }
    }
    
    /**
     * Gets a specific command by sequence number.
     */
    public HistoryEntry getCommandBySequence(int sequenceNumber) {
        synchronized (entries) {
            return entries.stream()
                .filter(entry -> entry.sequenceNumber == sequenceNumber)
                .findFirst()
                .orElse(null);
        }
    }
    
    /**
     * Searches command history by pattern.
     */
    public List<HistoryEntry> searchCommands(String pattern) {
        synchronized (entries) {
            return entries.stream()
                .filter(entry -> entry.commandLine.contains(pattern))
                .toList();
        }
    }
    
    /**
     * Gets session statistics.
     */
    public SessionStats getStats() {
        synchronized (entries) {
            long successCount = entries.stream()
                .filter(entry -> entry.result != null && entry.result.isSuccess())
                .count();
            
            long errorCount = entries.stream()
                .filter(entry -> entry.result != null && entry.result.isError())
                .count();
            
            long warningCount = entries.stream()
                .filter(entry -> entry.result != null && entry.result.isWarning())
                .count();
            
            return new SessionStats(
                entries.size(),
                successCount,
                errorCount,
                warningCount,
                sessionStart,
                java.time.Duration.between(sessionStart, Instant.now())
            );
        }
    }
    
    /**
     * Clears the command history.
     */
    public void clear() {
        synchronized (entries) {
            entries.clear();
            commandCounter.set(0);
            sessionStart = Instant.now();
        }
    }
    
    /**
     * Gets the current command count.
     */
    public int getCommandCount() {
        return commandCounter.get();
    }
    
    /**
     * Adds an entry to the history, maintaining size limits.
     */
    private void addEntry(HistoryEntry entry) {
        synchronized (entries) {
            entries.add(entry);
            
            // Maintain history size limit
            while (entries.size() > maxHistorySize) {
                entries.remove(0);
            }
        }
    }
    
    /**
     * Represents a single entry in the command history.
     */
    public record HistoryEntry(
        int sequenceNumber,
        String commandLine,
        Instant timestamp,
        CLIContext.CommandResult result
    ) {
        
        /**
         * Gets the execution duration if result is available.
         */
        public java.time.Duration getExecutionDuration() {
            if (result != null) {
                return java.time.Duration.between(timestamp, result.timestamp());
            }
            return null;
        }
        
        /**
         * Checks if this command was successful.
         */
        public boolean wasSuccessful() {
            return result != null && result.isSuccess();
        }
        
        /**
         * Gets a formatted string representation of this entry.
         */
        public String toFormattedString() {
            var sb = new StringBuilder();
            sb.append(String.format("%4d  %s  %s", 
                sequenceNumber, 
                timestamp.toString(), 
                commandLine));
            
            if (result != null) {
                sb.append(" -> ").append(result.status());
                if (result.message() != null) {
                    sb.append(" (").append(result.message()).append(")");
                }
            }
            
            return sb.toString();
        }
    }
    
    /**
     * Session statistics summary.
     */
    public record SessionStats(
        int totalCommands,
        long successfulCommands,
        long failedCommands,
        long warningCommands,
        Instant sessionStart,
        java.time.Duration sessionDuration
    ) {
        
        /**
         * Gets the success rate as a percentage.
         */
        public double getSuccessRate() {
            if (totalCommands == 0) return 0.0;
            return (double) successfulCommands / totalCommands * 100.0;
        }
        
        /**
         * Gets a formatted summary of the session statistics.
         */
        public String toFormattedString() {
            return String.format(
                "Session Stats: %d commands, %.1f%% success rate, duration: %s",
                totalCommands,
                getSuccessRate(),
                sessionDuration
            );
        }
    }
}