# Aether Infrastructure: Aspect

Cross-cutting concern aspects for [Aether](https://github.com/siy/aether) distributed runtime.

## Installation

```xml
<dependency>
    <groupId>org.pragmatica-lite.aether</groupId>
    <artifactId>infra-aspect</artifactId>
    <version>${aether.version}</version>
</dependency>
```

## Overview

Provides aspect factories for common cross-cutting concerns: circuit breaker, retry, logging, metrics, and transactions.

### Available Aspects

| Aspect | Description |
|--------|-------------|
| Circuit Breaker | Prevents cascading failures with open/closed/half-open states |
| Retry | Automatic retry with configurable backoff |
| Logging | Request/response logging with configurable levels |
| Metrics | Execution timing and counting |
| Transaction | Transaction boundary management |

### Circuit Breaker

```java
var factory = CircuitBreakerFactory.circuitBreakerFactory();

var config = CircuitBreakerConfig.circuitBreakerConfig()
    .failureThreshold(5)           // Open after 5 failures
    .successThreshold(3)           // Close after 3 successes in half-open
    .openDuration(Duration.ofSeconds(30))
    .build();

var aspect = factory.<MyService>create(config);
var wrapped = aspect.apply(myService);
```

### Retry

```java
var factory = RetryAspectFactory.retryAspectFactory();

var config = RetryConfig.retryConfig()
    .maxAttempts(3)
    .initialDelay(Duration.ofMillis(100))
    .maxDelay(Duration.ofSeconds(5))
    .multiplier(2.0)
    .build();

var aspect = factory.<MyService>create(config);
```

### Logging

```java
var factory = LoggingAspectFactory.loggingAspectFactory();

var config = LogConfig.logConfig()
    .level(LogLevel.INFO)
    .logRequest(true)
    .logResponse(true)
    .logDuration(true)
    .build();

var aspect = factory.<MyService>create(config);
```

### Metrics

```java
var factory = MetricsAspectFactory.metricsAspectFactory();

var config = MetricsConfig.metricsConfig()
    .prefix("my_service")
    .recordDuration(true)
    .recordCount(true)
    .build();

var aspect = factory.<MyService>create(config);
```

### Transaction

```java
var factory = TransactionAspectFactory.transactionAspectFactory();

var config = TransactionConfig.transactionConfig()
    .propagation(TransactionPropagation.REQUIRED)
    .isolation(IsolationLevel.READ_COMMITTED)
    .timeout(Duration.ofSeconds(30))
    .build();

var aspect = factory.<MyService>create(config);
```

### Composing Aspects

Aspects can be composed for layered behavior:

```java
var circuitBreaker = CircuitBreakerFactory.circuitBreakerFactory().create();
var retry = RetryAspectFactory.retryAspectFactory().create();
var logging = LoggingAspectFactory.loggingAspectFactory().create();

// Apply in order: logging -> retry -> circuit breaker -> target
var wrapped = logging.apply(retry.apply(circuitBreaker.apply(target)));
```

## License

Apache License 2.0
