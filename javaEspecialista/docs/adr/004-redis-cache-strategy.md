# ADR-004: Redis Cache Strategy

## Status

Accepted

## Context

Card lookup queries hit the database for every request. Since cards are immutable once registered, lookups are ideal for caching. We need a distributed cache to support horizontal scaling.

## Decision

We use **Redis** as a distributed cache layer with the following strategy:

### Positive Caching

- **On card registration**: Cache `hash → UUID` mapping with TTL.
- **On card lookup hit**: Cache the result with TTL.

### Negative Caching

- **On card lookup miss**: Cache `hash → NOT_FOUND` with shorter TTL.
- **Rationale**: Prevents repeated database hits for non-existent cards (common in batch processing with validation errors).

### Configuration

```yaml
app:
  cache:
    card:
      ttl: 3600  # 1 hour for positive cache
```

## Rationale

- **Distributed**: Redis supports multiple application instances, unlike in-process caches (Caffeine, Guava).
- **TTL-based expiration**: Self-cleaning, no manual invalidation needed for immutable data.
- **Negative caching**: Reduces database load from repeated lookups of non-existent cards, especially during batch file processing.

## Alternatives Considered

- **Caffeine (in-process)**: Faster but doesn't share state across instances. Acceptable for single-instance deployments.
- **Spring Cache abstraction with Redis**: More abstract but loses control over negative caching behavior.
- **Database query caching (MySQL)**: Less predictable and harder to configure per-query.

## Consequences

- Redis must be available (handled by circuit breaker on the repository adapter).
- Cache entries survive application restarts, which is desirable for immutable data.
- Negative cache entries auto-expire, preventing stale negatives from blocking future registrations.
