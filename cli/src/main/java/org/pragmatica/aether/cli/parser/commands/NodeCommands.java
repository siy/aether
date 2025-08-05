package org.pragmatica.aether.cli.parser.commands;

import org.pragmatica.aether.cli.parser.CommandParser;
import org.pragmatica.aether.cli.session.CLIContext;

import java.util.Map;

/**
 * Handles node-specific operations and status commands.
 * 
 * Node commands provide access to:
 * - Individual node status and health information
 * - Node resource utilization and performance metrics
 * - Node-specific configuration and settings
 * - Node deployment and runtime information
 * 
 * These commands focus on single-node operations and monitoring,
 * complementing the cluster-wide commands for detailed node inspection.
 */
public class NodeCommands implements CommandCategory {
    private final CLIContext context;
    
    public NodeCommands(CLIContext context) {
        this.context = context;
    }
    
    @Override
    public CommandParser.Command parseCommand(String[] args) {
        if (args.length == 0) {
            return new NodeStatusCommand(context);
        }
        
        var subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "status" -> new NodeStatusCommand(context);
            case "info" -> new NodeInfoCommand(context);
            case "health" -> new NodeHealthCommand(context);
            case "metrics" -> new NodeMetricsCommand(context);
            case "config" -> new NodeConfigCommand(context);
            case "resources" -> new NodeResourcesCommand(context);
            case "help" -> new NodeHelpCommand();
            default -> new UnknownNodeCommand(subCommand);
        };
    }
    
    @Override
    public Map<String, String> getCommands() {
        return Map.of(
            "status", "Show node status",
            "info", "Display node information",
            "health", "Show node health metrics",
            "metrics", "Display performance metrics",
            "config", "Show node configuration",
            "resources", "Display resource utilization",
            "help", "Show node command help"
        );
    }
    
    @Override
    public String getCategoryName() {
        return "node";
    }
    
    @Override
    public String getHelpText() {
        return """
            Node Commands:
            
              /node status          Show current node status
              /node info            Display detailed node information
              /node health          Show node health and diagnostics
              /node metrics         Display performance metrics
              /node config          Show node configuration
              /node resources       Display resource utilization
            
            Examples:
              /node status
              /node health
            """;
    }
    
    private static class NodeStatusCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public NodeStatusCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            var connectionManager = context.getConnectionManager();
            var activeConnection = connectionManager.getActiveConnection();
            
            if (activeConnection == null) {
                return CLIContext.CommandResult.error("No active connection to any node");
            }
            
            var stats = activeConnection.getStats();
            var sb = new StringBuilder();
            sb.append("Node Status:\n");
            sb.append("  ").append(stats.getFormattedStats()).append("\n");
            
            return CLIContext.CommandResult.success(sb.toString());
        }
        
        @Override
        public String getDescription() {
            return "Show node status";
        }
    }
    
    private static class NodeInfoCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public NodeInfoCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.success(
                "Node Info: [Placeholder - would show detailed node information]"
            );
        }
        
        @Override
        public String getDescription() {
            return "Display node information";
        }
    }
    
    private static class NodeHealthCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public NodeHealthCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.success(
                "Node Health: [Placeholder - would show health diagnostics]"
            );
        }
        
        @Override
        public String getDescription() {
            return "Show node health";
        }
    }
    
    private static class NodeMetricsCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public NodeMetricsCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.success(
                "Node Metrics: [Placeholder - would show performance metrics]"
            );
        }
        
        @Override
        public String getDescription() {
            return "Display performance metrics";
        }
    }
    
    private static class NodeConfigCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public NodeConfigCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.success(
                "Node Config: [Placeholder - would show node configuration]"
            );
        }
        
        @Override
        public String getDescription() {
            return "Show node configuration";
        }
    }
    
    private static class NodeResourcesCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public NodeResourcesCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.success(
                "Node Resources: [Placeholder - would show resource utilization]"
            );
        }
        
        @Override
        public String getDescription() {
            return "Display resource utilization";
        }
    }
    
    private static class NodeHelpCommand implements CommandParser.Command {
        @Override
        public CLIContext.CommandResult execute() {
            var commands = new NodeCommands(null);
            return CLIContext.CommandResult.success(commands.getHelpText());
        }
        
        @Override
        public String getDescription() {
            return "Show node help";
        }
    }
    
    private static class UnknownNodeCommand implements CommandParser.Command {
        private final String subCommand;
        
        public UnknownNodeCommand(String subCommand) {
            this.subCommand = subCommand;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.error(
                "Unknown node command: " + subCommand + "\nType '/node help' for available commands."
            );
        }
        
        @Override
        public String getDescription() {
            return "Unknown node command: " + subCommand;
        }
    }
}