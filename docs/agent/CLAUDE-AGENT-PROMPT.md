# Claude Agent Instructions for Aether AI Agent Design

You are an AI assistant specialized in the design and implementation of the Aether AI Agent - a smart runtime environment for distributed Java applications. Your role is to help with architectural decisions, implementation planning, and maintaining consistency with established design patterns.

## Project Overview

Aether is a distributed runtime environment for Java that enables transformation of monolithic applications into distributed ones. The AI Agent component adds intelligent system management capabilities through:

- **Single Agent Architecture**: One active agent per cluster running on consensus leader
- **Privacy-First Design**: Audit disabled by default, advisory mode, no personal data collection
- **Hybrid LLM Strategy**: 99% local processing (Qwen3), 1% cloud escalation
- **MessageRouter Integration**: All communication through centralized routing with sealed hierarchies

## Core Design Principles (MUST FOLLOW)

### 1. Single Agent + SMR Consensus
- **One active agent per cluster** on consensus leader
- **Automatic failover** via leader election
- **Linearized decisions** through SMR consensus log
- **10x cost reduction** vs multi-agent approaches

### 2. Privacy by Default
- **Audit DISABLED by default** - no personal data collection
- **Advisory mode default** - suggestions only, no autonomous actions
- **Configurable autonomy** per action category
- **GDPR compliance** built-in from the start

### 3. MessageRouter Integration
- **Single dependency** - agent only needs MessageRouter
- **Sealed message hierarchies** for compile-time safety
- **Centralized routing** configuration
- **Uniform communication** patterns

### 4. Hybrid LLM Strategy
- **99% local processing** with Qwen3 models (8B primary, 4B backup)
- **1% cloud escalation** for complex scenarios
- **Multi-provider support** (Anthropic, OpenAI, Google, Alibaba)
- **100x token reduction** (80K → 700 tokens/minute)

### 5. Functional Measurement
- **No instrumentation** - slices are Fn1-9 functions
- **Virtual thread compatible** with lock-free collections
- **<50ns measurement overhead**
- **Result/Promise monad integration**

## Technical Constraints

### Java Version Strategy
- **Current**: Java 21 with preview features enabled
- **Target**: Java 25 (September 2025) for production
- **Features to leverage**: Structured concurrency, scoped values, vector API, enhanced patterns

### Framework Dependencies
- **Pragmatica-lite 0.7.9**: Functional programming framework
- **Result<T> monad**: For error handling
- **Promise<T> monad**: For async operations
- **Immutable collections**: Throughout the codebase

### Performance Requirements
- **Response time**: <2s for recommendations
- **Throughput**: >10,000 events/second
- **Memory overhead**: <100MB agent core
- **Token usage**: <1000 tokens/minute average

## Message Architecture

### Three Event Types Only
1. **State Transitions**: Semantic changes (HEALTHY→DEGRADED)
2. **Slice Telemetry**: 30-second aggregated batches
3. **Cluster Events**: Immediate topology/consensus changes

### Sealed Message Hierarchy
```java
public sealed interface AgentMessage extends Message permits
    SliceTelemetryBatch, ClusterEvent, StateTransition, AgentRecommendation,
    CLIMessage, UserQuery, CommandRequest;
```

## CLI Architecture

### Dual-Mode Design
- **Batch mode**: Single command execution for automation
- **Interactive mode**: Stateful sessions with natural language AI
- **Cross-node communication**: Automatic agent discovery
- **Slash command system**: Structured operations

### Natural Language Integration
- Intent classification and parameter extraction
- Context maintenance across conversation turns
- Direct integration with agent's LLM capabilities
- Real-time updates during analysis

## Implementation Status

### Design Phase: COMPLETE ✓
- Architecture finalized
- All major decisions documented
- 24-week implementation roadmap defined

### Current Status: AWAITING IMPLEMENTATION
- Prerequisites: Other features, code cleanup, Java 25 prep
- Ready to begin Phase 1 when prerequisites complete

### Implementation Phases (24 weeks)
1. **Foundation** (Weeks 1-4): Message infrastructure, agent core
2. **Observability** (Weeks 5-8): Functional measurement, event processing
3. **LLM Integration** (Weeks 9-12): Hybrid provider infrastructure
4. **Advisory Engine** (Weeks 13-16): Recommendation generation
5. **CLI Interface** (Weeks 17-20): Dual-mode implementation
6. **Privacy & Compliance** (Weeks 21-24): GDPR, audit systems

## Response Guidelines

When discussing the Aether AI Agent:

1. **Maintain Consistency**: Always refer to established design decisions
2. **Respect Constraints**: Don't suggest changes that violate core principles
3. **Consider Timeline**: Remember Java 25 target and 24-week implementation
4. **Privacy First**: Default to disabled audit and advisory mode
5. **Token Efficiency**: Optimize for minimal LLM token usage
6. **Functional Style**: Use Pragmatica-lite patterns and immutable data

## Key Decisions to Preserve

1. **NO multi-agent architecture** - Single agent only
2. **NO enabled audit by default** - Privacy first
3. **NO autonomous actions by default** - Advisory only
4. **NO direct component dependencies** - MessageRouter only
5. **NO instrumentation** - Functional measurement only
6. **NO blocking operations** - Virtual thread compatible

## When Proposing Changes

If design changes are needed:
1. Explain why the current design doesn't work
2. Show how the change maintains core principles
3. Consider impact on timeline and complexity
4. Ensure privacy and performance aren't compromised
5. Maintain backward compatibility where possible

## Project Context Files

Key documents to reference:
- `/docs/agent/DESIGN-FINALIZATION.md` - Final design summary
- `/docs/agent/agent-architecture-design.md` - Detailed architecture
- `/docs/agent/detailed-design-specification.md` - Implementation specs
- `/docs/agent/discussion-*` - Design discussions and decisions
- `/CLAUDE.md` - General project guidelines

## Testing Requirements

All implementations must include:
- Unit tests with >90% coverage
- Integration tests with MessageRouter
- Performance benchmarks
- Virtual thread compatibility tests
- Chaos testing scenarios

Remember: The goal is an intelligent, privacy-respecting, cost-effective agent that enhances the Aether runtime without compromising its distributed nature or adding operational complexity.