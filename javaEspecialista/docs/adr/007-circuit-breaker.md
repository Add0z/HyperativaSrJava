# ADR-007: Circuit Breaker with Resilience4j

## Status

Accepted

## Context

The persistence adapter depends on both MySQL and Redis. If either becomes unavailable, the application could exhaust thread pools, connection pools, and memory waiting for timeouts. We need a mechanism to fail fast and recover gracefully.

## Decision

We use **Resilience4j Circuit Breaker** and **Retry** annotations on all `CardRepositoryAdapter` methods.

### Implementation Details

- **Circuit Breaker name**: `database`
- **Annotations**: `@CircuitBreaker(name = DATABASE)` + `@Retry(name = DATABASE)` on `save()`, `findUuidByHash()`, `deleteByHash()`.
- **Configuration**: Parameterized via `application.yml`:
  - Failure rate threshold
  - Wait duration in open state (half-open timeout)
  - Sliding window size

### State Machine

```
CLOSED → (failure threshold exceeded) → OPEN
OPEN   → (wait duration elapsed)      → HALF_OPEN
HALF_OPEN → (probe succeeds)          → CLOSED
HALF_OPEN → (probe fails)             → OPEN
```

## Rationale

- **Fail fast**: Prevents cascading failures when the database is down.
- **Resource protection**: Releases threads and connections immediately instead of blocking on timeouts.
- **Automatic recovery**: Half-open state allows the system to heal without manual intervention.
- **Retry resilience**: Transient errors (network blips) are handled by the retry mechanism before the circuit breaker opens.

## Alternatives Considered

- **Spring Retry only**: No circuit-breaking capability; retries would continue indefinitely during outages.
- **Hystrix**: Deprecated; Resilience4j is its recommended successor.
- **Custom implementation**: Unnecessary given Resilience4j's maturity and Spring Boot integration.

## Consequences

- All repository methods may throw `CallNotPermittedException` when the circuit is open; the global exception handler returns `503 Service Unavailable`.
- Monitoring circuit breaker state via Micrometer metrics is available out-of-the-box.
