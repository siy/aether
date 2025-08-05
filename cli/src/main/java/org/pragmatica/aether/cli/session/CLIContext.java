package org.pragmatica.aether.cli.session;

import org.pragmatica.aether.cli.AetherCLI;
import org.pragmatica.aether.cli.connection.ConnectionManager;
import org.pragmatica.aether.cli.parser.CommandParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central context for CLI operations, managing session state, connections, and command execution.
 * 
 * The CLIContext serves as the primary coordination point for all CLI functionality:
 * - Session management for interactive mode
 * - Connection management to cluster nodes
 * - Command parsing and execution coordination
 * - Resource lifecycle management
 * 
 * This context integrates with the existing Aether infrastructure while providing
 * a clean interface for CLI operations. It maintains connections to cluster nodes
 * and provides automatic agent discovery for seamless integration.
 */
public class CLIContext {
    private static final Logger logger = LoggerFactory.getLogger(CLIContext.class);
    
    private final AetherCLI.CLIArguments arguments;
    private final ConnectionManager connectionManager;
    private final CommandParser commandParser;
    private final SessionHistory sessionHistory;
    private final AtomicBoolean isShutdown;
    private final Instant creationTime;
    
    private CLIContext(AetherCLI.CLIArguments arguments) {
        this.arguments = arguments;
        this.connectionManager = new ConnectionManager();
        this.commandParser = new CommandParser(this);
        this.sessionHistory = new SessionHistory();
        this.isShutdown = new AtomicBoolean(false);
        this.creationTime = Instant.now();
        
        logger.info("CLI context created with arguments: {}", arguments);
    }
    
    /**
     * Creates a new CLI context with the specified arguments.
     */
    public static CLIContext create(AetherCLI.CLIArguments arguments) {
        return new CLIContext(arguments);
    }
    
    /**
     * Initializes the CLI context and establishes initial connections if specified.
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("Initializing CLI context");
                
                // Initialize connection manager
                connectionManager.initialize();
                
                // Connect to target node if specified
                if (arguments.targetNode() != null) {
                    connectionManager.connectToNode(arguments.targetNode())
                        .join(); // Block on initial connection for batch mode
                }
                
                logger.info("CLI context initialized successfully");
                
            } catch (Exception e) {
                logger.error("Failed to initialize CLI context: {}", e.getMessage(), e);
                throw new RuntimeException("CLI context initialization failed", e);
            }
        });
    }
    
    /**
     * Executes a command within this context.
     */
    public CompletableFuture<CommandResult> executeCommand(String commandLine) {
        if (isShutdown.get()) {
            return CompletableFuture.completedFuture(
                CommandResult.error("CLI context is shut down")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Add to session history
                sessionHistory.addCommand(commandLine);
                
                // Parse and execute command
                var command = commandParser.parse(commandLine);
                var result = command.execute();
                
                // Add result to history
                sessionHistory.addResult(result);
                
                logger.debug("Command executed: {} -> {}", commandLine, result.status());
                return result;
                
            } catch (Exception e) {
                logger.error("Command execution failed: {}", e.getMessage(), e);
                var errorResult = CommandResult.error("Command execution failed: " + e.getMessage());
                sessionHistory.addResult(errorResult);
                return errorResult;
            }
        });
    }
    
    /**
     * Gets the connection manager for cluster communication.
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }
    
    /**
     * Gets the command parser for parsing CLI commands.
     */
    public CommandParser getCommandParser() {
        return commandParser;
    }
    
    /**
     * Gets the session history for interactive mode.
     */
    public SessionHistory getSessionHistory() {
        return sessionHistory;
    }
    
    /**
     * Gets the CLI arguments used to create this context.
     */
    public AetherCLI.CLIArguments getArguments() {
        return arguments;
    }
    
    /**
     * Checks if the context is in interactive mode.
     */
    public boolean isInteractiveMode() {
        return arguments.isInteractiveMode();
    }
    
    /**
     * Checks if the context has been shut down.
     */
    public boolean isShutdown() {
        return isShutdown.get();
    }
    
    /**
     * Gets the uptime of this CLI context.
     */
    public java.time.Duration getUptime() {
        return java.time.Duration.between(creationTime, Instant.now());
    }
    
    /**
     * Shuts down the CLI context and cleans up resources.
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            logger.info("Shutting down CLI context after uptime: {}", getUptime());
            
            try {
                // Shutdown connection manager
                connectionManager.shutdown();
                
                // Clear session history if needed
                if (!isInteractiveMode()) {
                    sessionHistory.clear();
                }
                
                logger.info("CLI context shutdown completed");
                
            } catch (Exception e) {
                logger.error("Error during CLI context shutdown: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Result of command execution.
     */
    public static class CommandResult {
        private final CommandStatus status;
        private final String message;
        private final String output;
        private final Exception error;
        private final Instant timestamp;
        
        private CommandResult(CommandStatus status, String message, String output, Exception error) {
            this.status = status;
            this.message = message;
            this.output = output;
            this.error = error;
            this.timestamp = Instant.now();
        }
        
        public static CommandResult success(String output) {
            return new CommandResult(CommandStatus.SUCCESS, "Command executed successfully", output, null);
        }
        
        public static CommandResult success(String message, String output) {
            return new CommandResult(CommandStatus.SUCCESS, message, output, null);
        }
        
        public static CommandResult error(String message) {
            return new CommandResult(CommandStatus.ERROR, message, null, null);
        }
        
        public static CommandResult error(String message, Exception error) {
            return new CommandResult(CommandStatus.ERROR, message, null, error);
        }
        
        public static CommandResult warning(String message, String output) {
            return new CommandResult(CommandStatus.WARNING, message, output, null);
        }
        
        // Getters
        public CommandStatus status() { return status; }
        public String message() { return message; }
        public String output() { return output; }
        public Exception error() { return error; }
        public Instant timestamp() { return timestamp; }
        
        public boolean isSuccess() { return status == CommandStatus.SUCCESS; }
        public boolean isError() { return status == CommandStatus.ERROR; }
        public boolean isWarning() { return status == CommandStatus.WARNING; }
    }
    
    /**
     * Status of command execution.
     */
    public enum CommandStatus {
        SUCCESS,
        WARNING,
        ERROR
    }
}