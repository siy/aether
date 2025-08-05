package org.pragmatica.aether.cli;

import org.pragmatica.aether.cli.mode.BatchMode;
import org.pragmatica.aether.cli.mode.InteractiveMode;
import org.pragmatica.aether.cli.session.CLIContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Main entry point for the Aether CLI.
 * 
 * The CLI operates in two modes:
 * 1. Batch Mode: Execute commands from arguments or scripts and exit
 * 2. Interactive Mode: Start a REPL session for ongoing interaction
 * 
 * The CLI provides structured command access through slash commands organized by categories:
 * - /cluster: Cluster-wide operations and information
 * - /node: Node-specific operations and status
 * - /slice: Slice deployment and management
 * - /agent: Agent configuration and monitoring
 * 
 * Connection management automatically discovers available cluster nodes and maintains
 * connections for command execution. The CLI integrates with the existing MessageRouter
 * system for cluster communication.
 */
public class AetherCLI {
    private static final Logger logger = LoggerFactory.getLogger(AetherCLI.class);
    
    private static final String VERSION = "0.1.0";
    private static final String BANNER = """
        
         █████╗ ███████╗████████╗██╗  ██╗███████╗██████╗     ██████╗██╗     ██╗
        ██╔══██╗██╔════╝╚══██╔══╝██║  ██║██╔════╝██╔══██╗   ██╔════╝██║     ██║
        ███████║█████╗     ██║   ███████║█████╗  ██████╔╝   ██║     ██║     ██║
        ██╔══██║██╔══╝     ██║   ██╔══██║██╔══╝  ██╔══██╗   ██║     ██║     ██║
        ██║  ██║███████╗   ██║   ██║  ██║███████╗██║  ██║   ╚██████╗███████╗██║
        ╚═╝  ╚═╝╚══════╝   ╚═╝   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝    ╚═════╝╚══════╝╚═╝
        
        Aether Distributed Runtime CLI v%s
        """.formatted(VERSION);
    
    public static void main(String[] args) {
        try {
            var cli = new AetherCLI();
            cli.run(args);
        } catch (Exception e) {
            logger.error("CLI execution failed: {}", e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Main execution method that determines whether to run in batch or interactive mode.
     */
    public void run(String[] args) {
        // Parse command line arguments
        var cliArgs = parseArguments(args);
        
        // Initialize CLI context
        var context = CLIContext.create(cliArgs);
        
        try {
            if (cliArgs.isInteractiveMode()) {
                runInteractiveMode(context);
            } else {
                runBatchMode(context, cliArgs.commands());
            }
        } catch (Exception e) {
            logger.error("CLI execution error: {}", e.getMessage(), e);
            throw new RuntimeException("CLI execution failed", e);
        } finally {
            context.shutdown();
        }
    }
    
    /**
     * Runs the CLI in interactive mode with REPL functionality.
     */
    private void runInteractiveMode(CLIContext context) {
        System.out.println(BANNER);
        System.out.println("Type /help for available commands or /quit to exit.\n");
        
        var interactiveMode = new InteractiveMode(context);
        interactiveMode.start();
    }
    
    /**
     * Runs the CLI in batch mode to execute specific commands.
     */
    private void runBatchMode(CLIContext context, String[] commands) {
        var batchMode = new BatchMode(context);
        batchMode.executeCommands(commands);
    }
    
    /**
     * Parses command line arguments to determine execution mode and parameters.
     */
    private CLIArguments parseArguments(String[] args) {
        if (args.length == 0) {
            return new CLIArguments(
                true,  // interactive mode
                null,  // no target node
                null,  // no script file
                new String[0]  // no commands
            );
        }
        
        // Parse arguments for batch mode or connection parameters
        String targetNode = null;
        String scriptFile = null;
        boolean interactive = false;
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i", "--interactive" -> interactive = true;
                case "-n", "--node" -> {
                    if (i + 1 < args.length) {
                        targetNode = args[++i];
                    }
                }
                case "-f", "--file" -> {
                    if (i + 1 < args.length) {
                        scriptFile = args[++i];
                    }
                }
                case "-h", "--help" -> {
                    printUsage();
                    System.exit(0);
                }
                case "--version" -> {
                    System.out.println("Aether CLI version " + VERSION);
                    System.exit(0);
                }
            }
        }
        
        // If no specific mode flags, treat remaining args as commands for batch mode
        if (!interactive && scriptFile == null) {
            final String finalTargetNode = targetNode; // Make effectively final for lambda
            var commands = Arrays.stream(args)
                .filter(arg -> !arg.startsWith("-"))
                .filter(arg -> !arg.equals(finalTargetNode))
                .toArray(String[]::new);
            
            return new CLIArguments(false, targetNode, null, commands);
        }
        
        return new CLIArguments(interactive, targetNode, scriptFile, new String[0]);
    }
    
    /**
     * Prints usage information.
     */
    private void printUsage() {
        System.out.println("""
            Usage: aether-cli [OPTIONS] [COMMANDS...]
            
            Options:
              -i, --interactive    Start in interactive mode (default if no commands)
              -n, --node <address> Connect to specific node (host:port)
              -f, --file <path>    Execute commands from script file
              -h, --help           Show this help message
              --version            Show version information
            
            Interactive Mode Commands:
              /help                Show available commands
              /connect <node>      Connect to cluster node
              /disconnect          Disconnect from cluster
              /cluster <cmd>       Cluster operations
              /node <cmd>          Node operations
              /slice <cmd>         Slice management
              /agent <cmd>         Agent operations
              /quit, /exit         Exit CLI
            
            Examples:
              aether-cli                           # Interactive mode
              aether-cli -n localhost:8080         # Interactive with specific node
              aether-cli "/cluster status"         # Batch mode command
              aether-cli -f script.cli             # Execute script file
            """);
    }
    
    /**
     * Command line arguments container.
     */
    public record CLIArguments(
        boolean isInteractiveMode,
        String targetNode,
        String scriptFile,
        String[] commands
    ) {}
}