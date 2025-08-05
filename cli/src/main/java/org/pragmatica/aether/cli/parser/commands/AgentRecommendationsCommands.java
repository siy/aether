package org.pragmatica.aether.cli.parser.commands;

import org.pragmatica.aether.agent.message.AgentRecommendation;
import org.pragmatica.aether.cli.session.CLIContext;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * CLI commands for interacting with agent recommendations.
 * 
 * Provides comprehensive access to agent-generated recommendations,
 * supporting the Phase 1 integration demonstration. These commands
 * show how Track C (CLI) displays data from Track A (messaging) 
 * and Track B (LLM provider) integration.
 * 
 * Commands support:
 * - Listing recent recommendations with filtering
 * - Displaying detailed recommendation information  
 * - Monitoring recommendation metrics and costs
 * - Managing recommendation lifecycle
 * - Feature toggle integration for recommendation control
 */
public class AgentRecommendationsCommands {
    
    private final CLIContext context;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public AgentRecommendationsCommands(CLIContext context) {
        this.context = context;
    }
    
    /**
     * Lists recent agent recommendations.
     * Usage: /agent recommendations [limit] [priority] [type]
     */
    public String listRecommendations(String[] args) {
        var limit = args.length > 0 ? parseInt(args[0], 10) : 10;
        var priorityFilter = args.length > 1 ? args[1].toUpperCase() : null;
        var typeFilter = args.length > 2 ? args[2].toUpperCase() : null;
        
        try {
            // In Phase 1, we'll simulate this data
            var recommendations = getSimulatedRecommendations(limit);
            
            if (recommendations.isEmpty()) {
                return "No recommendations available. The agent may not be active or recommendations may be disabled.";
            }
            
            var filtered = recommendations.stream()
                .filter(rec -> priorityFilter == null || rec.priority().toString().equals(priorityFilter))
                .filter(rec -> typeFilter == null || rec.type().toString().contains(typeFilter))
                .limit(limit)
                .toList();
            
            return formatRecommendationsList(filtered);
            
        } catch (Exception e) {
            return "Error retrieving recommendations: " + e.getMessage();
        }
    }
    
    /**
     * Shows detailed information about a specific recommendation.
     * Usage: /agent recommendation <id>
     */
    public String showRecommendation(String[] args) {
        if (args.length == 0) {
            return "Usage: /agent recommendation <recommendation-id>";
        }
        
        var recommendationId = args[0];
        
        try {
            var recommendation = findRecommendation(recommendationId);
            if (recommendation == null) {
                return "Recommendation '" + recommendationId + "' not found.";
            }
            
            return formatRecommendationDetails(recommendation);
            
        } catch (Exception e) {
            return "Error retrieving recommendation: " + e.getMessage();
        }
    }
    
    /**
     * Shows agent recommendation metrics and statistics.
     * Usage: /agent recommendations metrics
     */
    public String showRecommendationMetrics(String[] args) {
        try {
            var metrics = getSimulatedMetrics();
            return formatMetrics(metrics);
            
        } catch (Exception e) {
            return "Error retrieving metrics: " + e.getMessage();
        }
    }
    
    /**
     * Shows the status of recommendation features and toggles.
     * Usage: /agent recommendations status
     */
    public String showRecommendationStatus(String[] args) {
        try {
            var status = getSimulatedStatus();
            return formatStatus(status);
            
        } catch (Exception e) {
            return "Error retrieving status: " + e.getMessage();
        }
    }
    
    /**
     * Filters recommendations by various criteria.
     * Usage: /agent recommendations filter <criteria> <value>
     */
    public String filterRecommendations(String[] args) {
        if (args.length < 2) {
            return """
                Usage: /agent recommendations filter <criteria> <value>
                
                Available criteria:
                - priority: HIGH, MEDIUM, LOW
                - type: PERFORMANCE_OPTIMIZATION, SCALING_DECISION, ISSUE_RESOLUTION, GENERAL_ADVICE
                - status: ACTIVE, COMPLETED, DISMISSED, ERROR, DISABLED
                - age: NUMBER (hours)
                """;
        }
        
        var criteria = args[0].toLowerCase();
        var value = args[1];
        
        try {
            var recommendations = getSimulatedRecommendations(50);
            var filtered = switch (criteria) {
                case "priority" -> recommendations.stream()
                    .filter(r -> r.priority().toString().equalsIgnoreCase(value))
                    .toList();
                case "type" -> recommendations.stream()
                    .filter(r -> r.type().toString().toLowerCase().contains(value.toLowerCase()))
                    .toList();
                case "status" -> recommendations.stream()
                    .filter(r -> r.status().toString().equalsIgnoreCase(value))
                    .toList();
                case "age" -> {
                    var maxAgeHours = parseInt(value, 24);
                    var cutoff = java.time.Instant.now().minusSeconds(maxAgeHours * 3600);
                    yield recommendations.stream()
                        .filter(r -> r.createdAt().isAfter(cutoff))
                        .toList();
                }
                default -> {
                    yield List.<AgentRecommendation>of();
                }
            };
            
            if (filtered.isEmpty()) {
                return "No recommendations match the filter criteria.";
            }
            
            return "Filtered Results (" + filtered.size() + " recommendations):\n\n" + 
                   formatRecommendationsList(filtered);
            
        } catch (Exception e) {
            return "Error filtering recommendations: " + e.getMessage();
        }
    }
    
    /**
     * Shows help for recommendation commands.
     */
    public String showHelp() {
        return """
            Agent Recommendations Commands:
            
            /agent recommendations [limit] [priority] [type]
                List recent recommendations with optional filtering
                
            /agent recommendation <id>
                Show detailed information about a specific recommendation
                
            /agent recommendations metrics
                Display recommendation generation metrics and costs
                
            /agent recommendations status
                Show recommendation system status and feature toggles
                
            /agent recommendations filter <criteria> <value>
                Filter recommendations by priority, type, status, or age
                
            Examples:
                /agent recommendations 5 HIGH
                /agent recommendation rec-123
                /agent recommendations filter priority HIGH
                /agent recommendations filter age 2
            """;
    }
    
    private String formatRecommendationsList(List<AgentRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return "No recommendations found.";
        }
        
        var sb = new StringBuilder();
        sb.append("Recent Agent Recommendations:\n");
        sb.append("=" .repeat(60)).append("\n\n");
        
        for (var rec : recommendations) {
            var status = formatStatus(rec.status());
            var priority = formatPriority(rec.priority());
            var time = rec.createdAt().atZone(java.time.ZoneId.systemDefault()).format(TIME_FORMATTER);
            
            sb.append(String.format("%-12s %s %s [%s]\n", 
                rec.id(), priority, status, time));
            sb.append(String.format("  %s: %s\n", 
                formatType(rec.type()), rec.title()));
            
            // Show preview of recommendation
            var preview = rec.recommendation().length() > 80 ? 
                rec.recommendation().substring(0, 80) + "..." : 
                rec.recommendation();
            sb.append(String.format("  → %s\n", preview));
            sb.append("\n");
        }
        
        sb.append("Use '/agent recommendation <id>' for detailed information.");
        return sb.toString();
    }
    
    private String formatRecommendationDetails(AgentRecommendation recommendation) {
        var sb = new StringBuilder();
        
        sb.append("Recommendation Details\n");
        sb.append("=" .repeat(50)).append("\n\n");
        
        sb.append("ID: ").append(recommendation.id()).append("\n");
        sb.append("Title: ").append(recommendation.title()).append("\n");
        sb.append("Type: ").append(formatType(recommendation.type())).append("\n");
        sb.append("Priority: ").append(formatPriority(recommendation.priority())).append("\n");
        sb.append("Status: ").append(formatStatus(recommendation.status())).append("\n");
        sb.append("Created: ").append(recommendation.createdAt().atZone(java.time.ZoneId.systemDefault()).format(DATE_FORMATTER)).append("\n");
        
        if (recommendation.validUntil() != null) {
            var validUntil = recommendation.createdAt().plus(recommendation.validUntil());
            sb.append("Valid Until: ").append(validUntil.atZone(java.time.ZoneId.systemDefault()).format(DATE_FORMATTER)).append("\n");
        }
        
        sb.append("\nRecommendation:\n");
        sb.append("-" .repeat(30)).append("\n");
        sb.append(recommendation.recommendation()).append("\n\n");
        
        if (!recommendation.metadata().isEmpty()) {
            sb.append("Metadata:\n");
            sb.append("-" .repeat(30)).append("\n");
            recommendation.metadata().forEach((key, value) -> 
                sb.append(String.format("  %-20s: %s\n", key, value)));
        }
        
        if (!recommendation.suggestedActions().isEmpty()) {
            sb.append("\nSuggested Actions:\n");
            sb.append("-" .repeat(30)).append("\n");
            for (int i = 0; i < recommendation.suggestedActions().size(); i++) {
                sb.append(String.format("  %d. %s\n", i + 1, recommendation.suggestedActions().get(i)));
            }
        }
        
        return sb.toString();
    }
    
    private String formatMetrics(Map<String, Object> metrics) {
        var sb = new StringBuilder();
        
        sb.append("Agent Recommendation Metrics\n");
        sb.append("=" .repeat(40)).append("\n\n");
        
        sb.append(String.format("Total Recommendations: %s\n", metrics.get("totalRecommendations")));
        sb.append(String.format("Success Rate: %.1f%%\n", (Double) metrics.get("successRate") * 100));
        sb.append(String.format("Total Cost: $%.4f\n", metrics.get("totalCost")));
        sb.append(String.format("Average Response Time: %s\n", metrics.get("averageResponseTime")));
        sb.append(String.format("Last Recommendation: %s\n", metrics.get("lastRecommendationTime")));
        
        sb.append("\nRecommendations by Priority:\n");
        sb.append(String.format("  High Priority: %s\n", metrics.get("highPriorityCount")));
        sb.append(String.format("  Medium Priority: %s\n", metrics.get("mediumPriorityCount")));
        sb.append(String.format("  Low Priority: %s\n", metrics.get("lowPriorityCount")));
        
        sb.append("\nRecommendations by Type:\n");
        sb.append(String.format("  Performance: %s\n", metrics.get("performanceCount")));
        sb.append(String.format("  Scaling: %s\n", metrics.get("scalingCount")));
        sb.append(String.format("  Issue Resolution: %s\n", metrics.get("issueCount")));
        sb.append(String.format("  General Advice: %s\n", metrics.get("generalCount")));
        
        return sb.toString();
    }
    
    private String formatStatus(Map<String, Object> status) {
        var sb = new StringBuilder();
        
        sb.append("Agent Recommendation Status\n");
        sb.append("=" .repeat(40)).append("\n\n");
        
        sb.append(String.format("Agent Status: %s\n", status.get("agentStatus")));
        sb.append(String.format("LLM Provider: %s\n", status.get("llmProvider")));
        sb.append(String.format("LLM Health: %s\n", status.get("llmHealth")));
        
        sb.append("\nFeature Toggles:\n");
        sb.append(String.format("  Recommendations Enabled: %s\n", status.get("recommendationsEnabled")));
        sb.append(String.format("  LLM Local Enabled: %s\n", status.get("llmLocalEnabled")));
        sb.append(String.format("  LLM Cloud Enabled: %s\n", status.get("llmCloudEnabled")));
        sb.append(String.format("  Cost Limits Enabled: %s\n", status.get("costLimitsEnabled")));
        sb.append(String.format("  Emergency Mode: %s\n", status.get("emergencyMode")));
        
        return sb.toString();
    }
    
    private String formatType(AgentRecommendation.RecommendationType type) {
        return switch (type) {
            case INVESTIGATION_REQUIRED -> "Investigation";
            case SCALING_RECOMMENDATION -> "Scaling";
            case EMERGENCY_ACTION -> "Emergency";
            case OPERATIONAL_GUIDANCE -> "Guidance";
            case RESOURCE_ADJUSTMENT -> "Resource";
            case CONFIGURATION_OPTIMIZATION -> "Config";
            case RESTART_RECOMMENDATION -> "Restart";
            case PREVENTIVE_MAINTENANCE -> "Maintenance";
            case CAPACITY_PLANNING -> "Capacity";
            case SECURITY_ACTION -> "Security";
        };
    }
    
    private String formatPriority(AgentRecommendation.RiskLevel riskLevel) {
        return switch (riskLevel) {
            case CRITICAL -> "🔴 CRITICAL";
            case HIGH -> "🟠 HIGH";
            case MEDIUM -> "🟡 MED";
            case LOW -> "🟢 LOW";
        };
    }
    
    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    // Simulation methods for Phase 1 - these would be replaced with actual service calls
    private List<AgentRecommendation> getSimulatedRecommendations(int limit) {
        var now = java.time.Instant.now();
        
        return List.of(
            AgentRecommendation.create(
                "rec-001",
                AgentRecommendation.RecommendationType.INVESTIGATION_REQUIRED,
                "High CPU Usage Alert",
                "The payment-service slice is experiencing high CPU usage (94%). This is caused by inefficient database queries in the transaction validation logic. Recommend implementing connection pooling and query optimization.",
                0.90,
                List.of(),
                Map.of("analysisType", "telemetry", "sliceCount", 5, "llmModel", "simple-mock-model"),
                java.time.Duration.ofHours(24),
                List.of("payment-service"),
                AgentRecommendation.RiskLevel.HIGH
            ),
            AgentRecommendation.create(
                "rec-002", 
                AgentRecommendation.RecommendationType.SCALING_RECOMMENDATION,
                "Scaling Recommendation",
                "Based on current load patterns, recommend scaling the user-management slice from 3 to 5 replicas during peak hours (9 AM - 6 PM). This will improve response times by approximately 40%.",
                0.85,
                List.of(),
                Map.of("analysisType", "telemetry", "currentReplicas", 3, "recommendedReplicas", 5),
                java.time.Duration.ofHours(12),
                List.of("user-management"),
                AgentRecommendation.RiskLevel.MEDIUM
            ),
            AgentRecommendation.create(
                "rec-003",
                AgentRecommendation.RecommendationType.EMERGENCY_ACTION,
                "Memory Leak Detection",
                "Analysis indicates a potential memory leak in the notification-service. The issue appears to be unclosed database connections. Immediate action required to prevent service failure.",
                0.95,
                List.of(),
                Map.of("analysisType", "telemetry", "memoryUsage", "95%", "heapUsage", "92%"),
                java.time.Duration.ofHours(6),
                List.of("notification-service"),
                AgentRecommendation.RiskLevel.CRITICAL
            )
        );
    }
    
    private AgentRecommendation findRecommendation(String id) {
        return getSimulatedRecommendations(10).stream()
            .filter(rec -> rec.id().equals(id))
            .findFirst()
            .orElse(null);
    }
    
    private Map<String, Object> getSimulatedMetrics() {
        return Map.of(
            "totalRecommendations", 47L,
            "successRate", 0.96,
            "totalCost", 0.0234,
            "averageResponseTime", "245ms",
            "lastRecommendationTime", "2 minutes ago",
            "highPriorityCount", 8,
            "mediumPriorityCount", 23,
            "lowPriorityCount", 16,
            "performanceCount", 18,
            "scalingCount", 12,
            "issueCount", 9,
            "generalCount", 8
        );
    }
    
    private Map<String, Object> getSimulatedStatus() {
        return Map.of(
            "agentStatus", "ACTIVE (Leader)",
            "llmProvider", "SimpleMockLLMProvider",
            "llmHealth", "HEALTHY", 
            "recommendationsEnabled", true,
            "llmLocalEnabled", true,
            "llmCloudEnabled", false,
            "costLimitsEnabled", true,
            "emergencyMode", false
        );
    }
}