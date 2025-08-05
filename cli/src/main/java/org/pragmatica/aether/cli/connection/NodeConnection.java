package org.pragmatica.aether.cli.connection;

import org.pragmatica.aether.agent.AetherAgent;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.message.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a connection to a single Aether cluster node.
 * 
 * This class encapsulates:
 * - Network connection management to a specific node
 * - MessageRouter integration for cluster communication
 * - Agent discovery results and cached information
 * - Connection health monitoring and statistics
 * 
 * The NodeConnection provides a clean abstraction for CLI operations
 * to interact with cluster nodes, handling the underlying complexity
 * of network communication and agent coordination.
 */
public class NodeConnection {
    private static final Logger logger = LoggerFactory.getLogger(NodeConnection.class);
    
    private final NodeId nodeId;
    private final InetSocketAddress address;
    private final AtomicBoolean connected;
    private final AtomicReference<MessageRouter> messageRouter;
    private final AtomicReference<List<AgentInfo>> discoveredAgents;
    private volatile Instant connectionTime;
    private volatile Instant lastActivity;
    
    public NodeConnection(NodeId nodeId, InetSocketAddress address) {
        this.nodeId = nodeId;
        this.address = address;
        this.connected = new AtomicBoolean(false);
        this.messageRouter = new AtomicReference<>();
        this.discoveredAgents = new AtomicReference<>();
    }
    
    /**
     * Establishes connection to the node.
     */
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connected.get()) {
                    logger.debug("Already connected to node: {}", nodeId);
                    return;
                }
                
                logger.info("Establishing connection to node {} at {}", nodeId, address);
                
                // In a full implementation, this would establish actual network connection
                // For now, simulate connection establishment
                simulateConnection();
                
                connected.set(true);
                connectionTime = Instant.now();
                lastActivity = Instant.now();
                
                logger.info("Successfully connected to node: {}", nodeId);
                
            } catch (Exception e) {
                logger.error("Failed to connect to node {}: {}", nodeId, e.getMessage(), e);
                throw new RuntimeException("Connection failed", e);
            }
        });
    }
    
    /**
     * Disconnects from the node.
     */
    public void disconnect() {
        if (connected.compareAndSet(true, false)) {
            try {
                logger.info("Disconnecting from node: {}", nodeId);
                
                // Cleanup message router
                var router = messageRouter.getAndSet(null);
                if (router != null) {
                    // In a full implementation, would properly shutdown router
                }
                
                // Clear discovered agents
                discoveredAgents.set(null);
                
                logger.info("Disconnected from node: {}", nodeId);
                
            } catch (Exception e) {
                logger.error("Error disconnecting from node {}: {}", nodeId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Sends a message through this connection.
     */
    public <T> CompletableFuture<T> sendMessage(Object message, Class<T> responseType) {
        if (!connected.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Not connected to node: " + nodeId)
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                lastActivity = Instant.now();
                
                // In a full implementation, would use MessageRouter to send message
                // For now, simulate message sending
                logger.debug("Sending message to node {}: {}", nodeId, message.getClass().getSimpleName());
                
                // Simulate response (placeholder)
                return simulateResponse(responseType);
                
            } catch (Exception e) {
                logger.error("Failed to send message to node {}: {}", nodeId, e.getMessage(), e);
                throw new RuntimeException("Message sending failed", e);
            }
        });
    }
    
    /**
     * Checks if the connection is healthy and active.
     */
    public boolean isConnected() {
        return connected.get();
    }
    
    /**
     * Gets the node ID.
     */
    public NodeId getNodeId() {
        return nodeId;
    }
    
    /**
     * Gets the node address.
     */
    public InetSocketAddress getAddress() {
        return address;
    }
    
    /**
     * Gets the connection uptime.
     */
    public Duration getUptime() {
        return connectionTime != null ? 
            Duration.between(connectionTime, Instant.now()) : 
            Duration.ZERO;
    }
    
    /**
     * Gets the time since last activity.
     */
    public Duration getIdleTime() {
        return lastActivity != null ? 
            Duration.between(lastActivity, Instant.now()) : 
            Duration.ZERO;
    }
    
    /**
     * Sets the discovered agents for this node.
     */
    public void setDiscoveredAgents(List<AgentInfo> agents) {
        discoveredAgents.set(agents);
        logger.debug("Updated discovered agents for node {}: {} agents", nodeId, agents.size());
    }
    
    /**
     * Gets the discovered agents on this node.
     */
    public List<AgentInfo> getDiscoveredAgents() {
        var agents = discoveredAgents.get();
        return agents != null ? List.copyOf(agents) : List.of();
    }
    
    /**
     * Gets connection statistics.
     */
    public ConnectionStats getStats() {
        return new ConnectionStats(
            nodeId,
            address,
            connected.get(),
            connectionTime,
            lastActivity,
            getUptime(),
            getIdleTime(),
            getDiscoveredAgents().size()
        );
    }
    
    /**
     * Simulates connection establishment (placeholder for actual implementation).
     */
    private void simulateConnection() {
        // In a full implementation, this would:
        // 1. Establish network connection using Netty
        // 2. Initialize MessageRouter with the connection
        // 3. Perform handshake and authentication
        // 4. Set up message handling
        
        // For now, just simulate the delay
        try {
            Thread.sleep(100); // Simulate connection time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Connection interrupted", e);
        }
    }
    
    /**
     * Simulates response generation (placeholder for actual implementation).
     */
    @SuppressWarnings("unchecked")
    private <T> T simulateResponse(Class<T> responseType) {
        // In a full implementation, this would handle actual message routing
        // For now, return null or empty responses based on type
        
        if (responseType == String.class) {
            return (T) "OK";
        } else if (responseType == Boolean.class) {
            return (T) Boolean.TRUE;
        }
        
        return null;
    }
    
    /**
     * Information about an agent discovered on this node.
     */
    public record AgentInfo(
        NodeId nodeId,
        AetherAgent.AgentState state,
        boolean isLeader,
        Instant startTime
    ) {
        
        public boolean isActive() {
            return state == AetherAgent.AgentState.ACTIVE;
        }
        
        public Duration getUptime() {
            return startTime != null ? 
                Duration.between(startTime, Instant.now()) : 
                Duration.ZERO;
        }
    }
    
    /**
     * Connection statistics and health information.
     */
    public record ConnectionStats(
        NodeId nodeId,
        InetSocketAddress address,
        boolean isConnected,
        Instant connectionTime,
        Instant lastActivity,
        Duration uptime,
        Duration idleTime,
        int discoveredAgents
    ) {
        
        public boolean isHealthy() {
            return isConnected && idleTime.toMinutes() < 5; // Healthy if active within 5 minutes
        }
        
        public String getFormattedStats() {
            return String.format(
                "Node: %s, Connected: %s, Uptime: %s, Idle: %s, Agents: %d",
                nodeId,
                isConnected,
                uptime,
                idleTime,
                discoveredAgents
            );
        }
    }
}