package org.pragmatica.aether.cli.mode;

import org.pragmatica.aether.cli.session.CLIContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Batch mode implementation for non-interactive CLI command execution.
 * 
 * Batch mode features:
 * - Execute single commands or command sequences
 * - Script file execution with error handling
 * - Structured output for automation and scripting
 * - Exit code management for integration with shell scripts
 * - Performance optimized for rapid command execution
 * 
 * Batch mode is designed for automation, CI/CD integration, and
 * scripted cluster management workflows.
 */
public class BatchMode {
    private static final Logger logger = LoggerFactory.getLogger(BatchMode.class);
    
    private final CLIContext context;
    private int exitCode = 0;
    
    public BatchMode(CLIContext context) {
        this.context = context;
    }
    
    /**
     * Executes a series of commands in batch mode.
     * 
     * @param commands Array of command strings to execute
     */
    public void executeCommands(String[] commands) {
        try {
            // Initialize context
            context.initialize().join();
            
            logger.info("Executing {} commands in batch mode", commands.length);
            
            for (int i = 0; i < commands.length; i++) {
                var command = commands[i];
                logger.debug("Executing command {}/{}: {}", i + 1, commands.length, command);
                
                var result = context.executeCommand(command).join();
                
                // Display result
                displayBatchResult(result, i + 1, commands.length);
                
                // Handle errors
                if (result.isError()) {
                    exitCode = 1;
                    
                    // In batch mode, continue with remaining commands unless critical error
                    if (isCriticalError(result)) {
                        logger.error("Critical error encountered, stopping batch execution");
                        break;
                    }
                }
            }
            
            logger.info("Batch execution completed with exit code: {}", exitCode);
            
        } catch (Exception e) {
            logger.error("Batch mode execution failed: {}", e.getMessage(), e);
            exitCode = 1;
            System.err.println("Batch execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Executes commands from a script file.
     * 
     * @param scriptPath Path to the script file
     */
    public void executeScript(String scriptPath) {
        try {
            var path = Path.of(scriptPath);
            
            if (!Files.exists(path)) {
                System.err.println("Script file not found: " + scriptPath);
                exitCode = 1;
                return;
            }
            
            logger.info("Executing script file: {}", scriptPath);
            
            var lines = Files.readAllLines(path);
            var commands = filterScriptLines(lines);
            
            executeCommands(commands.toArray(String[]::new));
            
        } catch (IOException e) {
            logger.error("Failed to read script file {}: {}", scriptPath, e.getMessage(), e);
            System.err.println("Failed to read script file: " + e.getMessage());
            exitCode = 1;
        }
    }
    
    /**
     * Gets the exit code for the batch execution.
     */
    public int getExitCode() {
        return exitCode;
    }
    
    /**
     * Displays the result of a batch command execution.
     */
    private void displayBatchResult(CLIContext.CommandResult result, int commandIndex, int totalCommands) {
        var prefix = String.format("[%d/%d]", commandIndex, totalCommands);
        
        switch (result.status()) {
            case SUCCESS -> {
                System.out.println(prefix + " SUCCESS");
                if (result.output() != null && !result.output().isEmpty()) {
                    // Indent output for clarity in batch mode
                    var indentedOutput = indentOutput(result.output());
                    System.out.println(indentedOutput);
                }
            }
            case WARNING -> {
                System.out.println(prefix + " WARNING: " + result.message());
                if (result.output() != null && !result.output().isEmpty()) {
                    var indentedOutput = indentOutput(result.output());
                    System.out.println(indentedOutput);
                }
            }
            case ERROR -> {
                System.err.println(prefix + " ERROR: " + result.message());
                if (result.error() != null) {
                    logger.debug("Command error details", result.error());
                }
            }
        }
    }
    
    /**
     * Checks if an error is critical enough to stop batch execution.
     */
    private boolean isCriticalError(CLIContext.CommandResult result) {
        // Define critical errors that should stop batch execution
        var message = result.message().toLowerCase();
        
        return message.contains("connection lost") ||
               message.contains("authentication failed") ||
               message.contains("cluster unavailable") ||
               message.contains("critical system error");
    }
    
    /**
     * Filters script lines to extract valid commands.
     */
    private List<String> filterScriptLines(List<String> lines) {
        return lines.stream()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .filter(line -> !line.startsWith("#"))  // Skip comments
            .filter(line -> !line.startsWith("//")) // Skip comments
            .toList();
    }
    
    /**
     * Indents output for better readability in batch mode.
     */
    private String indentOutput(String output) {
        return Arrays.stream(output.split("\n"))
            .map(line -> "  " + line)
            .reduce((a, b) -> a + "\n" + b)
            .orElse("");
    }
    
    /**
     * Advanced batch mode with enhanced features.
     */
    public static class AdvancedBatchMode extends BatchMode {
        private boolean continueOnError = false;
        private boolean verbose = false;
        
        public AdvancedBatchMode(CLIContext context) {
            super(context);
        }
        
        public AdvancedBatchMode setContinueOnError(boolean continueOnError) {
            this.continueOnError = continueOnError;
            return this;
        }
        
        public AdvancedBatchMode setVerbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }
        
        // Future enhancements:
        // - Parallel command execution for independent commands
        // - Conditional command execution
        // - Variable substitution in commands
        // - Command timeouts and retries
        // - Structured output formats (JSON, XML)
        // - Command dependency management
    }
}