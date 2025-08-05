package org.pragmatica.aether.cli.mode;

import org.pragmatica.aether.cli.session.CLIContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

/**
 * Interactive mode implementation providing REPL functionality for the CLI.
 * 
 * Interactive mode features:
 * - Real-time command input and execution
 * - Session history with command recall
 * - Contextual help and command completion
 * - Graceful error handling and recovery
 * - Clean shutdown on quit commands
 * 
 * The interactive mode maintains session state and provides a user-friendly
 * interface for ongoing interaction with the Aether cluster environment.
 */
public class InteractiveMode {
    private static final Logger logger = LoggerFactory.getLogger(InteractiveMode.class);
    
    private static final String PROMPT = "aether> ";
    private static final String CONTINUATION_PROMPT = "     -> ";
    
    private final CLIContext context;
    private final BufferedReader reader;
    private volatile boolean running;
    
    public InteractiveMode(CLIContext context) {
        this.context = context;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.running = false;
    }
    
    /**
     * Starts the interactive REPL session.
     */
    public void start() {
        try {
            running = true;
            
            // Initialize context
            context.initialize().join();
            
            logger.info("Starting interactive mode");
            
            // Main REPL loop
            while (running && !context.isShutdown()) {
                try {
                    var commandLine = readCommandLine();
                    
                    if (commandLine == null) {
                        // EOF encountered (Ctrl+D)
                        break;
                    }
                    
                    if (commandLine.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Execute command
                    var result = context.executeCommand(commandLine).join();
                    
                    // Display result
                    displayResult(result);
                    
                    // Check for quit command
                    if (isQuitCommand(commandLine)) {
                        break;
                    }
                    
                } catch (Exception e) {
                    logger.error("Error in interactive loop: {}", e.getMessage(), e);
                    System.err.println("Error: " + e.getMessage());
                }
            }
            
            displayGoodbye();
            
        } catch (Exception e) {
            logger.error("Interactive mode failed: {}", e.getMessage(), e);
            System.err.println("Interactive mode error: " + e.getMessage());
        } finally {
            running = false;
            cleanup();
        }
    }
    
    /**
     * Stops the interactive session.
     */
    public void stop() {
        running = false;
        logger.info("Interactive mode stop requested");
    }
    
    /**
     * Reads a command line from user input, handling multi-line commands.
     */
    private String readCommandLine() throws IOException {
        System.out.print(PROMPT);
        System.out.flush();
        
        var line = reader.readLine();
        
        if (line == null) {
            return null; // EOF
        }
        
        // Handle multi-line commands (future enhancement)
        // For now, just return single line
        return line;
    }
    
    /**
     * Displays command execution result to the user.
     */
    private void displayResult(CLIContext.CommandResult result) {
        switch (result.status()) {
            case SUCCESS -> {
                if (result.output() != null && !result.output().isEmpty()) {
                    System.out.println(result.output());
                }
            }
            case WARNING -> {
                System.out.println("Warning: " + result.message());
                if (result.output() != null && !result.output().isEmpty()) {
                    System.out.println(result.output());
                }
            }
            case ERROR -> {
                System.err.println("Error: " + result.message());
                if (result.error() != null) {
                    logger.debug("Command error details", result.error());
                }
            }
        }
    }
    
    /**
     * Checks if the command is a quit command.
     */
    private boolean isQuitCommand(String commandLine) {
        var trimmed = commandLine.trim().toLowerCase();
        return trimmed.equals("/quit") || trimmed.equals("/exit");
    }
    
    /**
     * Displays goodbye message and session statistics.
     */
    private void displayGoodbye() {
        System.out.println();
        
        var sessionStats = context.getSessionHistory().getStats();
        System.out.println("Session completed.");
        System.out.println(sessionStats.toFormattedString());
        
        var connectionInfo = context.getConnectionManager().getConnectionInfo();
        if (connectionInfo.hasActiveConnection()) {
            System.out.println("Disconnecting from cluster...");
        }
        
        System.out.println("Goodbye!");
    }
    
    /**
     * Cleanup resources used by interactive mode.
     */
    private void cleanup() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            logger.warn("Error closing reader: {}", e.getMessage());
        }
    }
    
    /**
     * Enhanced interactive mode with features like command completion and history navigation.
     * This is a placeholder for future enhancements.
     */
    public static class EnhancedInteractiveMode extends InteractiveMode {
        public EnhancedInteractiveMode(CLIContext context) {
            super(context);
        }
        
        // Future enhancements:
        // - Command completion using Tab
        // - History navigation with Up/Down arrows
        // - Syntax highlighting
        // - Multi-line command support
        // - Command suggestions
    }
}