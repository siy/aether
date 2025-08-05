package org.pragmatica.aether.cli.connection;

import org.pragmatica.aether.agent.AetherAgent;
import org.pragmatica.cluster.net.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Discovers and monitors Aether agents within connected cluster nodes.
 * 
 * The AgentDiscovery component provides:
 * - Automatic discovery of agents running on cluster nodes
 * - Agent health and status monitoring
 * - Leadership tracking for distributed agent coordination
 * - Integration with CLI connection management
 * 
 * This enables the CLI to automatically find and interact with agents
 * across the cluster, providing seamless access to agent functionality
 * regardless of which nodes are hosting active agents.
 */
public class AgentDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(AgentDiscovery.class);
    
    private final AtomicBoolean isShutdown;
    
    public AgentDiscovery() {
        this.isShutdown = new AtomicBoolean(false);
    }
    
    /**
     * Discovers agents running on the specified node connection.
     * 
     * @param connection The node connection to query for agents
     * @return Future containing list of discovered agents
     */
    public CompletableFuture<List<NodeConnection.AgentInfo>> discoverAgents(NodeConnection connection) {
        return CompletableFuture.supplyAsync(() -> {
            if (isShutdown.get()) {
                throw new IllegalStateException("Agent discovery is shut down");
            }
            
            try {
                logger.debug("Starting agent discovery for node: {}", connection.getNodeId());
                
                // In a full implementation, this would:
                // 1. Send discovery request through MessageRouter
                // 2. Query cluster topology for agent information
                // 3. Collect agent health and status data
                // 4. Identify current leader agent
                
                // For now, simulate discovery with mock data
                var agents = simulateAgentDiscovery(connection);
                
                logger.info("Discovered {} agents on node {}", agents.size(), connection.getNodeId());
                return agents;
                
            } catch (Exception e) {
                logger.error("Agent discovery failed for node {}: {}", connection.getNodeId(), e.getMessage(), e);
                throw new RuntimeException("Agent discovery failed", e);
            }
        });
    }
    
    /**
     * Refreshes agent information for all provided connections.
     * 
     * @param connections List of connections to refresh
     * @return Future completing when all discoveries are done
     */
    public CompletableFuture<Void> refreshAgentInfo(List<NodeConnection> connections) {
        if (isShutdown.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Agent discovery is shut down")
            );
        }
        
        var discoveryFutures = connections.stream()
            .map(this::discoverAgents)
            .map(future -> future.thenAccept(agents -> {
                // Update connection with discovered agents
                var connection = connections.stream()
                    .filter(conn -> conn.getNodeId().equals(agents.get(0).nodeId()))
                    .findFirst();
                
                connection.ifPresent(conn -> conn.setDiscoveredAgents(agents));
            }))
            .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(discoveryFutures);
    }
    
    /**
     * Finds the current leader agent across all connections.
     * 
     * @param connections List of connections to search
     * @return Future containing the leader agent info, or null if none found
     */
    public CompletableFuture<NodeConnection.AgentInfo> findLeaderAgent(List<NodeConnection> connections) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Searching for leader agent across {} connections", connections.size());
                
                for (var connection : connections) {
                    var agents = connection.getDiscoveredAgents();
                    var leader = agents.stream()
                        .filter(NodeConnection.AgentInfo::isLeader)
                        .filter(NodeConnection.AgentInfo::isActive)
                        .findFirst();
                    
                    if (leader.isPresent()) {
                        logger.info("Found leader agent on node: {}", connection.getNodeId());
                        return leader.get();
                    }
                }
                
                logger.warn("No leader agent found across {} connections", connections.size());
                return null;
                
            } catch (Exception e) {
                logger.error("Error finding leader agent: {}", e.getMessage(), e);
                return null;
            }
        });
    }
    
    /**
     * Gets health information for all discovered agents.
     * 
     * @param connections List of connections to check
     * @return Future containing agent health summary
     */
    public CompletableFuture<AgentHealthSummary> getAgentHealth(List<NodeConnection> connections) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int totalAgents = 0;
                int activeAgents = 0;
                int leaderAgents = 0;
                int healthyAgents = 0;
                
                for (var connection : connections) {
                    var agents = connection.getDiscoveredAgents();
                    totalAgents += agents.size();
                    
                    for (var agent : agents) {
                        if (agent.isActive()) {
                            activeAgents++;
                        }
                        if (agent.isLeader()) {
                            leaderAgents++;
                        }
                        if (agent.state() != AetherAgent.AgentState.FAILED) {
                            healthyAgents++;
                        }
                    }
                }
                
                return new AgentHealthSummary(
                    totalAgents,
                    activeAgents,
                    leaderAgents,
                    healthyAgents,
                    Instant.now()
                );
                
            } catch (Exception e) {
                logger.error("Error getting agent health: {}", e.getMessage(), e);
                return new AgentHealthSummary(0, 0, 0, 0, Instant.now());
            }
        });
    }
    
    /**
     * Shuts down the agent discovery service.
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            logger.info("Agent discovery service shutting down");
        }
    }
    
    /**
     * Simulates agent discovery for testing purposes.
     * In a full implementation, this would query actual cluster state.
     */
    private List<NodeConnection.AgentInfo> simulateAgentDiscovery(NodeConnection connection) {
        // Simulate finding one agent per node for now
        // In reality, there might be 0 or 1 agent per node depending on configuration
        
        var nodeId = connection.getNodeId();
        var agentInfo = new NodeConnection.AgentInfo(
            nodeId,
            AetherAgent.AgentState.ACTIVE,
            true, // Simulate this as the leader for now
            Instant.now().minus(Duration.ofMinutes(10)) // Started 10 minutes ago
        );
        
        return List.of(agentInfo);
    }
    
    /**
     * Summary of agent health across the cluster.
     */
    public record AgentHealthSummary(
        int totalAgents,
        int activeAgents,
        int leaderAgents,
        int healthyAgents,
        Instant checkTime
    ) {
        
        /**
         * Gets the percentage of healthy agents.
         */
        public double getHealthPercentage() {
            return totalAgents > 0 ? (double) healthyAgents / totalAgents * 100.0 : 0.0;
        }
        
        /**
         * Checks if the cluster has a healthy leader.
         */
        public boolean hasHealthyLeader() {
            return leaderAgents > 0;
        }
        
        /**
         * Gets a formatted health summary.
         */
        public String getFormattedSummary() {
            return String.format(
                "Agents: %d total, %d active, %d leaders, %.1f%% healthy (checked at %s)",
                totalAgents,
                activeAgents,
                leaderAgents,
                getHealthPercentage(),
                checkTime
            );
        }
    }
}