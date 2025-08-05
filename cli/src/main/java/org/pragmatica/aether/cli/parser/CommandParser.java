package org.pragmatica.aether.cli.parser;

import org.pragmatica.aether.cli.parser.commands.*;
import org.pragmatica.aether.cli.session.CLIContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses and routes CLI commands using a slash-command system.
 * 
 * The CommandParser provides structured command parsing with organized categories:
 * - /cluster: Cluster-wide operations and information
 * - /node: Node-specific operations and status  
 * - /slice: Slice deployment and management
 * - /agent: Agent configuration and monitoring
 * - /help: Built-in help and documentation
 * - /connect, /disconnect: Connection management
 * 
 * The parser uses a hierarchical approach where each category has its own
 * command handler, enabling extensible command organization and clear
 * separation of concerns. Commands are executed within the provided
 * CLIContext for access to connections and session state.
 */
public class CommandParser {
    private static final Logger logger = LoggerFactory.getLogger(CommandParser.class);
    
    private final CLIContext context;
    private final Map<String, CommandCategory> categories;
    
    public CommandParser(CLIContext context) {
        this.context = context;
        this.categories = new HashMap<>();
        
        initializeCommands();
    }
    
    /**
     * Parses a command line and returns an executable command.
     * 
     * @param commandLine The raw command line input
     * @return Parsed command ready for execution
     */
    public Command parse(String commandLine) {
        try {
            var trimmed = commandLine.trim();
            
            if (trimmed.isEmpty()) {
                return new EmptyCommand();
            }
            
            // Handle special cases first
            if (trimmed.equals("/quit") || trimmed.equals("/exit")) {
                return new QuitCommand();
            }
            
            if (trimmed.equals("/help") || trimmed.equals("/?")) {
                return new HelpCommand(categories);
            }
            
            // Parse slash commands
            if (trimmed.startsWith("/")) {
                return parseSlashCommand(trimmed);
            }
            
            // Handle natural language commands (future enhancement)
            return new UnknownCommand(commandLine);
            
        } catch (Exception e) {
            logger.error("Command parsing failed for '{}': {}", commandLine, e.getMessage(), e);
            return new ErrorCommand("Failed to parse command: " + e.getMessage());
        }
    }
    
    /**
     * Parses a slash command into its category and arguments.
     */
    private Command parseSlashCommand(String commandLine) {
        var parts = parseCommandParts(commandLine);
        
        if (parts.length == 0) {
            return new ErrorCommand("Empty command");
        }
        
        var categoryName = parts[0].substring(1); // Remove leading slash
        var category = categories.get(categoryName);
        
        if (category == null) {
            return new UnknownCommand(commandLine);
        }
        
        var args = Arrays.copyOfRange(parts, 1, parts.length);
        return category.parseCommand(args);
    }
    
    /**
     * Splits command line into parts, respecting quoted strings.
     */
    private String[] parseCommandParts(String commandLine) {
        // Simple split for now - in a full implementation would handle quoted strings properly
        return commandLine.split("\\s+");
    }
    
    /**
     * Initializes all command categories.
     */
    private void initializeCommands() {
        // Connection commands
        categories.put("connect", new ConnectionCommands(context));
        categories.put("disconnect", new ConnectionCommands(context));
        
        // Core functional categories
        categories.put("cluster", new ClusterCommands(context));
        categories.put("node", new NodeCommands(context));
        categories.put("slice", new SliceCommands(context));
        categories.put("agent", new AgentCommands(context));
        
        logger.debug("Initialized {} command categories", categories.size());
    }
    
    /**
     * Gets available command categories for help display.
     */
    public Map<String, CommandCategory> getCategories() {
        return Map.copyOf(categories);
    }
    
    /**
     * Base interface for all commands.
     */
    public interface Command {
        CLIContext.CommandResult execute();
        String getDescription();
    }
    
    /**
     * Command for empty input.
     */
    private static class EmptyCommand implements Command {
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.success("");
        }
        
        @Override
        public String getDescription() {
            return "Empty command";
        }
    }
    
    /**
     * Command to quit the CLI.
     */
    private static class QuitCommand implements Command {
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.success("quit", "Goodbye!");
        }
        
        @Override
        public String getDescription() {
            return "Exit the CLI";
        }
    }
    
    /**
     * Command to display help information.
     */
    private static class HelpCommand implements Command {
        private final Map<String, CommandCategory> categories;
        
        public HelpCommand(Map<String, CommandCategory> categories) {
            this.categories = categories;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            var help = buildHelpText();
            return CLIContext.CommandResult.success(help);
        }
        
        private String buildHelpText() {
            var sb = new StringBuilder();
            sb.append("Aether CLI Commands:\n\n");
            
            sb.append("General Commands:\n");
            sb.append("  /help, /?           Show this help message\n");
            sb.append("  /quit, /exit        Exit the CLI\n\n");
            
            sb.append("Connection Commands:\n");
            sb.append("  /connect <node>     Connect to cluster node\n");
            sb.append("  /disconnect         Disconnect from current node\n\n");
            
            sb.append("Cluster Commands:\n");
            categories.get("cluster").getCommands().forEach((cmd, desc) -> 
                sb.append(String.format("  /cluster %-10s %s\n", cmd, desc))
            );
            
            sb.append("\nNode Commands:\n");
            categories.get("node").getCommands().forEach((cmd, desc) -> 
                sb.append(String.format("  /node %-13s %s\n", cmd, desc))
            );
            
            sb.append("\nSlice Commands:\n");
            categories.get("slice").getCommands().forEach((cmd, desc) -> 
                sb.append(String.format("  /slice %-12s %s\n", cmd, desc))
            );
            
            sb.append("\nAgent Commands:\n");
            categories.get("agent").getCommands().forEach((cmd, desc) -> 
                sb.append(String.format("  /agent %-12s %s\n", cmd, desc))
            );
            
            sb.append("\nFor detailed help on a category, use: /category help\n");
            sb.append("Example: /cluster help\n");
            
            return sb.toString();
        }
        
        @Override
        public String getDescription() {
            return "Display help information";
        }
    }
    
    /**
     * Command for unknown input.
     */
    private static class UnknownCommand implements Command {
        private final String input;
        
        public UnknownCommand(String input) {
            this.input = input;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.error(
                "Unknown command: " + input + "\nType /help for available commands."
            );
        }
        
        @Override
        public String getDescription() {
            return "Unknown command: " + input;
        }
    }
    
    /**
     * Command for parsing errors.
     */
    private static class ErrorCommand implements Command {
        private final String error;
        
        public ErrorCommand(String error) {
            this.error = error;
        }
        
        @Override
        public CLIContext.CommandResult execute() {
            return CLIContext.CommandResult.error(error);
        }
        
        @Override
        public String getDescription() {
            return "Error: " + error;
        }
    }
}