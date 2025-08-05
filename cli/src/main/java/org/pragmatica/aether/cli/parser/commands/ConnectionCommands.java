package org.pragmatica.aether.cli.parser.commands;

import org.pragmatica.aether.cli.parser.CommandParser;
import org.pragmatica.aether.cli.session.CLIContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles connection management commands for the CLI.
 * 
 * Connection commands enable users to:
 * - Connect to specific cluster nodes
 * - Disconnect from current connections
 * - View connection status and health
 * - Manage connection settings
 * 
 * These commands integrate with the ConnectionManager to provide
 * seamless cluster connectivity for CLI operations.
 */
public class ConnectionCommands implements CommandCategory {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionCommands.class);
    
    private final CLIContext context;
    
    public ConnectionCommands(CLIContext context) {
        this.context = context;
    }
    
    @Override
    public CommandParser.Command parseCommand(String[] args) {
        if (args.length == 0) {
            return new ConnectionStatusCommand(context);
        }
        
        var subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "status" -> new ConnectionStatusCommand(context);
            case "list" -> new ListConnectionsCommand(context);
            case "help" -> new ConnectionHelpCommand();
            default -> {
                // If this is a connect command with node address
                if (args.length == 1 && !subCommand.equals("help")) {
                    yield new ConnectCommand(context, args[0]);
                }
                yield new UnknownConnectionCommand(subCommand);
            }
        };
    }
    
    @Override
    public Map<String, String> getCommands() {
        return Map.of(
            "status", "Show current connection status",
            "list", "List all connections",
            "<node>", "Connect to specified node (host:port)",
            "help", "Show connection command help"
        );
    }
    
    @Override
    public String getCategoryName() {
        return "connection";
    }
    
    @Override
    public String getHelpText() {
        return """
            Connection Commands:
            
              /connect <node>       Connect to cluster node (format: host:port)
              /connect status       Show current connection status
              /connect list         List all established connections
              /disconnect           Disconnect from current node
              /disconnect all       Disconnect from all nodes
            
            Examples:
              /connect localhost:8080
              /connect status
              /disconnect
            """;
    }
    
    /**
     * Command to connect to a specific node.
     */
    private static class ConnectCommand implements CommandParser.Command {
        private final CLIContext context;
        private final String nodeAddress;
        
        public ConnectCommand(CLIContext context, String nodeAddress) {
            this.context = context;
            this.nodeAddress = nodeAddress;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            try {
                var connectionManager = context.getConnectionManager();
                
                var connection = connectionManager.connectToNode(nodeAddress).join();
                
                return CLIContext.CommandResult.success(
                    "Connected to node: " + nodeAddress,
                    String.format("Successfully connected to %s\nNode ID: %s", 
                        nodeAddress, connection.getNodeId())
                );
                
            } catch (Exception e) {
                logger.error("Failed to connect to node {}: {}", nodeAddress, e.getMessage(), e);
                return CLIContext.CommandResult.error(
                    "Failed to connect to " + nodeAddress + ": " + e.getMessage()
                );
            }
        }
        
        @Override
        public String getDescription() {
            return "Connect to node: " + nodeAddress;
        }
    }
    
    /**
     * Command to show connection status.
     */
    private static class ConnectionStatusCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public ConnectionStatusCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            var connectionManager = context.getConnectionManager();
            var info = connectionManager.getConnectionInfo();
            
            var sb = new StringBuilder();
            sb.append("Connection Status:\n");
            sb.append(String.format("  Active Node: %s\n", 
                info.hasActiveConnection() ? info.activeNode() : "None"));
            sb.append(String.format("  Total Connections: %d\n", info.totalConnections()));
            sb.append(String.format("  Healthy Connections: %d\n", info.healthyConnections()));
            sb.append(String.format("  Health Ratio: %.1f%%\n", info.getHealthRatio() * 100));
            
            if (info.hasActiveConnection()) {
                var activeConnection = connectionManager.getActiveConnection();
                var stats = activeConnection.getStats();
                sb.append("\nActive Connection Details:\n");
                sb.append("  ").append(stats.getFormattedStats()).append("\n");
            }
            
            return CLIContext.CommandResult.success(sb.toString());
        }
        
        @Override
        public String getDescription() {
            return "Show connection status";
        }
    }
    
    /**
     * Command to list all connections.
     */
    private static class ListConnectionsCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public ListConnectionsCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            var connections = context.getConnectionManager().getAllConnections();
            
            if (connections.isEmpty()) {
                return CLIContext.CommandResult.success("No active connections");
            }
            
            var sb = new StringBuilder();
            sb.append("Active Connections:\n");
            
            for (var connection : connections) {
                var stats = connection.getStats();
                sb.append(String.format("  %s - %s\n", 
                    connection.getAddress(), 
                    stats.isHealthy() ? "Healthy" : "Unhealthy"));
                sb.append(String.format("    Uptime: %s, Agents: %d\n", 
                    stats.uptime(), 
                    stats.discoveredAgents()));
            }
            
            return CLIContext.CommandResult.success(sb.toString());
        }
        
        @Override
        public String getDescription() {
            return "List all connections";
        }
    }
    
    /**
     * Command to show connection help.
     */
    private static class ConnectionHelpCommand implements CommandParser.Command {
        @Override
        public CLIContext.CommandResult execute() {
            var commands = new ConnectionCommands(null);
            return CLIContext.CommandResult.success(commands.getHelpText());
        }
        
        @Override
        public String getDescription() {
            return "Show connection help";
        }
    }
    
    /**
     * Command for unknown connection subcommands.
     */
    private static class UnknownConnectionCommand implements CommandParser.Command {
        private final String subCommand;
        
        public UnknownConnectionCommand(String subCommand) {
            this.subCommand = subCommand;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.error(
                "Unknown connection command: " + subCommand + "\nType '/connect help' for available commands."
            );
        }
        
        @Override
        public String getDescription() {
            return "Unknown connection command: " + subCommand;
        }
    }
}