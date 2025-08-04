# Functional Measurement for AI Agent - Discussion Document
*Date: 2025-08-03 (Continuation)*

## Overview

This document captures the refined approach to instrumentation and measurement for the AI agent, based on the understanding that slices are functional interfaces (Fn1-Fn9) returning Result or Promise monads.

## Key Insight: No Instrumentation Needed

Since slices are just functions implementing Pragmatica-lite's `Fn1<Result<R>, T>` through `Fn9<Result<R>, T1, ..., T9>` interfaces, measurement can be achieved through simple functional composition without any bytecode instrumentation, AspectJ weaving, or reflection.

## Corrected Event Types for Agent

The AI agent receives exactly three types of events:

1. **State machine changes** - Node/slice/cluster health transitions
2. **Periodic slice telemetry** - Aggregated performance metrics (every 30s)
3. **Cluster events (IMMEDIATE)** - Topology and consensus changes

## Functional Measurement Implementation

### Core Measurement Pattern

```java
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Functions.Fn2;

// Simple wrapper for measurement
public <T, R> Fn1<Result<R>, T> measure(
        Fn1<Result<R>, T> slice, 
        SliceId id) {
    return input -> {
        long startNanos = System.nanoTime();
        
        return slice.apply(input)
            .onSuccess(result -> {
                long duration = System.nanoTime() - startNanos;
                collector.record(id, duration);
            })
            .onFailure(cause -> {
                collector.recordFailure(id, cause);
            });
    };
}
```

### Virtual Thread Compatibility

**Problem**: Traditional ThreadLocal approach fails with virtual threads (potential millions of threads).

**Solution**: Use striped collection with ConcurrentHashMap and atomic adders:

```java
public class LockFreeCollector {
    private final ConcurrentHashMap<SliceId, SliceMetrics> metrics = 
        new ConcurrentHashMap<>();
    
    static class SliceMetrics {
        private final LongAdder invocations = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();
        private final Recorder latencyRecorder = new Recorder(...);
        
        void recordLockFree(long nanos) {
            invocations.increment();
            totalNanos.add(nanos);
            latencyRecorder.recordValue(nanos);
        }
    }
}
```

## Slice Telemetry Structure

```java
record SliceTelemetry(
    SliceId slice,
    Duration period,                // Measurement window (30s)
    long invocationCount,          // Total calls in period
    Duration totalDuration,        // Sum of all execution times
    double avgDuration,            // Mean execution time
    double p99Duration,            // 99th percentile latency
    long memoryAllocated,          // Estimated memory usage
    double cpuSeconds              // Estimated CPU time
)
```

## Integration with Pragmatica-lite

### Slice Registry Pattern

```java
public class SliceRegistry {
    // Register slice with automatic measurement wrapping
    public <T, R> Result<Unit> registerSlice(
            SliceId id,
            Fn1<Result<R>, T> implementation) {
        
        return Result.lift(() -> {
            // Wrap with measurement
            Fn1<Result<R>, T> measured = measurement.measure(implementation, id);
            
            // Store and notify consensus
            slices.put(id, measured);
            consensusModule.propose(new SliceRegistered(id, nodeId));
            
            return Unit.UNIT;
        });
    }
}
```

### Resilient Execution

```java
public class ResilientSliceExecutor {
    private final CircuitBreaker breaker = CircuitBreaker.create(...);
    private final Retry retry = Retry.create(...);
    
    public <T, R> Promise<R> executeResilient(
            Fn1<Promise<R>, T> slice, 
            T input) {
        
        return breaker.protect(() ->
            retry.execute(() -> slice.apply(input))
        );
    }
}
```

### Periodic Reporting

```java
public class TelemetryReporter {
    public void start() {
        // Schedule with SharedScheduler every 30 seconds
        SharedScheduler.schedule(
            Duration.ofSeconds(30),
            this::reportTelemetry
        );
    }
    
    private void reportTelemetry() {
        // Collect metrics, create telemetry batch
        // Send through consensus to AI agent
        consensus.propose(new TelemetryBatch(telemetry));
    }
}
```

## Memory Allocation Tracking Challenges

**Problem**: ThreadMXBean.getCurrentThreadAllocatedBytes() doesn't work well with virtual threads.

**Solutions**:
1. **JFR Events**: Use Java Flight Recorder for precise allocation tracking
2. **Sampling**: Approximate allocation using heap pressure measurements
3. **Statistical**: Attribute overall memory pressure proportionally

## Performance Characteristics

### Benefits
- **Zero instrumentation overhead**: No bytecode manipulation
- **Type-safe**: Compiler-enforced correctness
- **Composable**: Standard functional composition
- **Virtual thread friendly**: No ThreadLocal dependencies
- **Lock-free collection**: Minimal contention

### Overhead Analysis
- **Function wrapping**: One-time cost per slice registration
- **Measurement**: ~10-50ns per invocation (System.nanoTime calls)
- **Collection**: Lock-free atomic operations
- **Reporting**: Batched every 30 seconds

## Integration Points with SMR

1. **Slice Registration**: New slices reported through consensus
2. **Telemetry Batching**: Periodic metrics sent via consensus
3. **State Transitions**: Slice health changes flow through consensus
4. **Agent Consumption**: Single agent receives all events in order

## Novel Aspects

This functional measurement approach is unique because it:
- Leverages functional composition instead of instrumentation
- Works naturally with monadic error handling
- Scales to virtual threads without ThreadLocal issues
- Provides type-safe measurement guarantees
- Integrates seamlessly with SMR consensus

## Implementation Priority

1. **Core measurement wrapper** - Basic Fn1/Fn2 measurement
2. **Lock-free collector** - Virtual thread compatible metrics
3. **Telemetry reporter** - Periodic consensus integration
4. **Slice registry** - Automatic measurement registration
5. **Resilience integration** - Circuit breaker + retry patterns

## Open Questions

1. **Memory tracking precision**: JFR vs sampling vs statistical approaches?
2. **Measurement granularity**: Should we measure individual slice methods or just entry points?
3. **Error categorization**: How detailed should failure cause tracking be?
4. **Performance tuning**: Optimal reporting intervals and batch sizes?

This functional approach eliminates the complexity of traditional instrumentation while providing comprehensive observability for the AI agent.