package org.pragmatica.aether.cli.parser.commands;

import org.pragmatica.aether.cli.parser.CommandParser;
import org.pragmatica.aether.cli.session.CLIContext;

import java.util.Map;

/**
 * Handles agent configuration and monitoring commands.
 * 
 * Agent commands provide functionality for:
 * - Agent status monitoring and health checks
 * - Agent configuration management
 * - Agent performance metrics and statistics
 * - Agent leadership and consensus information
 * 
 * These commands enable comprehensive management of the intelligent
 * agents running within the Aether distributed environment.
 */
public class AgentCommands implements CommandCategory {
    private final CLIContext context;
    
    public AgentCommands(CLIContext context) {
        this.context = context;
    }
    
    @Override
    public CommandParser.Command parseCommand(String[] args) {
        if (args.length == 0) {
            return new AgentStatusCommand(context);
        }
        
        var subCommand = args[0].toLowerCase();
        
        return switch (subCommand) {
            case "status" -> new AgentStatusCommand(context);
            case "health" -> new AgentHealthCommand(context);
            case "config" -> new AgentConfigCommand(context);
            case "metrics" -> new AgentMetricsCommand(context);
            case "leader" -> new AgentLeaderCommand(context);
            case "discover" -> new AgentDiscoverCommand(context);
            case "help" -> new AgentHelpCommand();
            default -> new UnknownAgentCommand(subCommand);
        };
    }
    
    @Override
    public Map<String, String> getCommands() {
        return Map.of(
            "status", "Show agent status",
            "health", "Show agent health metrics",
            "config", "Show agent configuration",
            "metrics", "Display agent performance metrics",
            "leader", "Show agent leadership information",
            "discover", "Discover agents in cluster",
            "help", "Show agent command help"
        );
    }
    
    @Override
    public String getCategoryName() {
        return "agent";
    }
    
    @Override
    public String getHelpText() {
        return """
            Agent Commands:
            
              /agent status         Show current agent status
              /agent health         Show agent health and diagnostics
              /agent config         Show agent configuration
              /agent metrics        Display agent performance metrics
              /agent leader         Show agent leadership information
              /agent discover       Discover agents across cluster nodes
            
            Examples:
              /agent status
              /agent health
            """;
    }
    
    private static class AgentStatusCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public AgentStatusCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            var connectionManager = context.getConnectionManager();
            var activeConnection = connectionManager.getActiveConnection();
            
            if (activeConnection == null) {
                return CLIContext.CommandResult.error("No active connection to query agent status");
            }
            
            var agents = activeConnection.getDiscoveredAgents();
            
            if (agents.isEmpty()) {
                return CLIContext.CommandResult.success("No agents discovered on connected node");
            }
            
            var sb = new StringBuilder();
            sb.append("Agent Status:\n");
            
            for (var agent : agents) {
                sb.append(String.format("  Node: %s\n", agent.nodeId()));
                sb.append(String.format("  State: %s\n", agent.state()));
                sb.append(String.format("  Leader: %s\n", agent.isLeader() ? "Yes" : "No"));
                sb.append(String.format("  Uptime: %s\n", agent.getUptime()));
                sb.append("\n");
            }
            
            return CLIContext.CommandResult.success(sb.toString());
        }
        
        @Override
        public String getDescription() {
            return "Show agent status";
        }
    }
    
    private static class AgentHealthCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public AgentHealthCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            var connectionManager = context.getConnectionManager();
            var connections = connectionManager.getAllConnections();
            
            if (connections.isEmpty()) {
                return CLIContext.CommandResult.error("No connections available for agent health check");
            }
            
            // In a full implementation, would use AgentDiscovery.getAgentHealth()
            return CLIContext.CommandResult.success(
                "Agent Health: [Placeholder - would show comprehensive health metrics]"
            );
        }
        
        @Override
        public String getDescription() {
            return "Show agent health";
        }
    }
    
    private static class AgentConfigCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public AgentConfigCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.success(
                "Agent Configuration: [Placeholder - would show agent config]"
            );
        }
        
        @Override
        public String getDescription() {
            return "Show agent configuration";
        }
    }
    
    private static class AgentMetricsCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public AgentMetricsCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.success(
                "Agent Metrics: [Placeholder - would show performance metrics]"
            );
        }
        
        @Override
        public String getDescription() {
            return "Show agent metrics";
        }
    }
    
    private static class AgentLeaderCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public AgentLeaderCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            var connectionManager = context.getConnectionManager();
            var connections = connectionManager.getAllConnections();
            
            if (connections.isEmpty()) {
                return CLIContext.CommandResult.error("No connections available to check leadership");
            }
            
            // Find leader agent across all connections
            for (var connection : connections) {
                var agents = connection.getDiscoveredAgents();
                var leader = agents.stream()
                    .filter(agent -> agent.isLeader() && agent.isActive())
                    .findFirst();
                
                if (leader.isPresent()) {
                    var agent = leader.get();
                    return CLIContext.CommandResult.success(
                        String.format("Agent Leader found on node %s:\n  State: %s\n  Uptime: %s", 
                            agent.nodeId(), agent.state(), agent.getUptime())
                    );
                }
            }
            
            return CLIContext.CommandResult.warning(
                "No active leader agent found across connected nodes",
                "This may indicate a leadership election in progress or cluster issues."
            );
        }
        
        @Override
        public String getDescription() {
            return "Show agent leadership information";
        }
    }
    
    private static class AgentDiscoverCommand implements CommandParser.Command {
        private final CLIContext context;
        
        public AgentDiscoverCommand(CLIContext context) {
            this.context = context;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            var connectionManager = context.getConnectionManager();
            var connections = connectionManager.getAllConnections();
            
            if (connections.isEmpty()) {
                return CLIContext.CommandResult.error("No connections available for agent discovery");
            }
            
            return CLIContext.CommandResult.success(
                "Agent Discovery: [Placeholder - would trigger fresh discovery across all nodes]"
            );
        }
        
        @Override
        public String getDescription() {
            return "Discover agents in cluster";
        }
    }
    
    private static class AgentHelpCommand implements CommandParser.Command {
        @Override
        public CLIContext.CommandResult execute() {
            var commands = new AgentCommands(null);
            return CLIContext.CommandResult.success(commands.getHelpText());
        }
        
        @Override
        public String getDescription() {
            return "Show agent help";
        }
    }
    
    private static class UnknownAgentCommand implements CommandParser.Command {
        private final String subCommand;
        
        public UnknownAgentCommand(String subCommand) {
            this.subCommand = subCommand;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.error(
                "Unknown agent command: " + subCommand + "\nType '/agent help' for available commands."
            );
        }
        
        @Override
        public String getDescription() {
            return "Unknown agent command: " + subCommand;
        }
    }
}