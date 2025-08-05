package org.pragmatica.aether.cli.parser.commands;

import org.pragmatica.aether.cli.parser.CommandParser;

import java.util.Map;

/**
 * Base interface for command categories in the CLI system.
 * 
 * Each command category represents a logical grouping of related commands
 * (e.g., cluster, node, slice, agent). Categories are responsible for:
 * - Parsing commands within their domain
 * - Providing help and documentation for their commands
 * - Managing command execution and validation
 * 
 * This design enables clean separation of concerns and extensible
 * command organization within the CLI framework.
 */
public interface CommandCategory {
    
    /**
     * Parses a command within this category.
     * 
     * @param args Command arguments (excluding the category name)
     * @return Executable command
     */
    CommandParser.Command parseCommand(String[] args);
    
    /**
     * Gets available commands in this category with their descriptions.
     * 
     * @return Map of command names to descriptions
     */
    Map<String, String> getCommands();
    
    /**
     * Gets the name of this command category.
     */
    String getCategoryName();
    
    /**
     * Gets detailed help text for this category.
     */
    String getHelpText();
}