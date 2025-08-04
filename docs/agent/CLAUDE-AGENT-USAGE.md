# How to Use the Aether AI Agent Design Assistant

This Claude agent is configured to help with the design and implementation of the Aether AI Agent component while maintaining consistency with established architectural decisions.

## Quick Start

1. **Load the agent configuration**:
   - Use the prompt from `CLAUDE-AGENT-PROMPT.md`
   - Reference the configuration in `CLAUDE-AGENT-CONFIG.json`
   - Ensure all context files are available

2. **Provide project context**:
   - The agent has knowledge of all design decisions
   - It understands the 24-week implementation roadmap
   - It knows the current status (design complete, awaiting implementation)

## What the Agent Can Help With

### 1. Architecture Questions
- Validate design decisions against core principles
- Explain why certain choices were made
- Suggest improvements within constraints
- Review architectural diagrams and flows

### 2. Implementation Planning
- Break down the 24-week roadmap into detailed tasks
- Identify dependencies and prerequisites
- Plan specific component implementations
- Create development schedules

### 3. Technical Design
- Design message types and hierarchies
- Plan LLM integration strategies
- Design functional measurement approaches
- Create API specifications

### 4. Code Review
- Ensure functional programming style
- Validate virtual thread compatibility
- Check MessageRouter integration patterns
- Review for privacy compliance

### 5. Documentation
- Keep design documents up to date
- Track design decisions and changes
- Create implementation guides
- Write API documentation

## Important Constraints the Agent Enforces

1. **Single Agent Architecture**: Will not suggest multi-agent designs
2. **Privacy First**: Audit disabled by default, advisory mode only
3. **MessageRouter Only**: No direct component dependencies
4. **Functional Style**: Immutable data, Result/Promise monads
5. **Token Efficiency**: Optimizes for <1000 tokens/minute

## Example Interactions

### Architecture Review
```
"Review this proposed change to the event processing pipeline. Does it maintain our core principles?"
```

### Implementation Planning
```
"Create a detailed task breakdown for Week 5-8 (Observability phase) of the implementation."
```

### Technical Design
```
"Design the sealed message hierarchy for CLI integration with the agent."
```

### Code Review
```
"Review this SliceTelemetryBatch implementation for virtual thread safety and functional style."
```

## Tips for Best Results

1. **Reference Specific Decisions**: Mention design documents when asking questions
2. **Provide Context**: Include relevant code or diagrams
3. **Be Specific**: Ask about particular components or phases
4. **Check Constraints**: Verify suggestions against core principles

## When the Agent Will Push Back

The agent will resist changes that:
- Enable audit by default
- Add autonomous actions without user consent
- Introduce multi-agent complexity
- Break MessageRouter isolation
- Add blocking operations incompatible with virtual threads
- Significantly increase token usage

## Updating the Agent

If design decisions change:
1. Update the relevant design documents
2. Modify `CLAUDE-AGENT-PROMPT.md` with new constraints
3. Adjust `CLAUDE-AGENT-CONFIG.json` validation rules
4. Document the rationale for changes

## Integration with Development

The agent is designed to support:
- Daily development decisions
- Code review processes
- Architecture discussions
- Documentation updates
- Implementation planning

Use it as a consistent reference point throughout the 24-week implementation journey.