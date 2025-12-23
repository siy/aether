# Development Priorities

## Current Status (v0.4.0)

Most foundational work is complete. Priorities have shifted to polish and AI integration.

## Completed ✅

- **Structured Keys** - KV schema foundation
- **Consensus Integration** - Distributed operations working
- **ClusterDeploymentManager** - Cluster orchestration
- **EndpointRegistry** - Service discovery
- **NodeDeploymentManager** - Node-level slice management
- **HTTP Router** - External request routing
- **Management API** - Cluster control endpoints
- **CLI** - REPL and batch modes
- **Demo App** - 5-slice order domain

## Current Priorities

### HIGH PRIORITY

1. **CLI Polish**
   - Improve command feedback
   - Add more diagnostic commands
   - Better error messages

2. **Agent API Documentation**
   - Document Management API endpoints
   - Create agent integration guide
   - Provide examples for direct API access

3. **Decision Tree Tuning**
   - Improve scaling rules
   - Add more health checks
   - Better failure recovery

### MEDIUM PRIORITY

4. **SharedLibraryClassLoader Optimization**
   - Test with more complex dependencies
   - Improve compatibility detection
   - Performance tuning

5. **Metrics Enhancement**
   - Add more slice-level metrics
   - Improve historical data storage
   - Better visualization support

### FUTURE (AI Integration)

6. **SLM Integration (Layer 2)**
   - Local model integration (Ollama)
   - Pattern learning
   - Anomaly detection

7. **LLM Integration (Layer 3)**
   - Claude/GPT API integration
   - Complex reasoning workflows
   - Multi-cloud decision support

## Deprecated

- **MCP Server** - Replaced by direct agent API (see [ai-integration.md](ai-integration.md))

## Implementation Approach

Focus on stability and documentation before adding AI layers:

1. CLI must be reliable for human operators
2. Agent API must be well-documented
3. Decision tree must handle all common cases
4. Only then add SLM/LLM layers

See [ai-integration.md](ai-integration.md) for AI integration architecture.
