# ADR-006: Distributed Rate Limiting

## Status

Accepted

## Context

The API handles sensitive financial data and must be protected against abuse, brute-force attacks, and DDoS. Rate limiting must work across multiple application instances (horizontal scaling), ruling out in-process solutions.

## Decision

We implement a **Redis-based distributed rate limiter** as a Servlet Filter (`RateLimitFilter`), using an atomic increment with TTL per client IP.

### Implementation Details

- **Key pattern**: `rate_limit:{clientIp}` with 1-minute TTL.
- **Algorithm**: Sliding window counter via `INCR` + `EXPIRE` on first request.
- **Limit**: Configurable via `app.ratelimit.requests-per-minute` (default: 100).
- **Response**: `429 Too Many Requests` with JSON body when exceeded.
- **Metrics**: `rate_limit_allowed_total` and `rate_limit_blocked_total` counters with `client_id` and `endpoint` tags.

## Rationale

- **Distributed**: Redis ensures rate limit state is shared across all application instances.
- **Low latency**: Single `INCR` operation per request (~0.1ms).
- **Self-cleaning**: TTL-based expiration, no manual cleanup required.
- **Observable**: Dedicated metrics for monitoring abuse patterns.

## Alternatives Considered

- **Bucket4j**: More sophisticated (token bucket algorithm) but adds a library dependency for a simple use case.
- **Spring Cloud Gateway Rate Limiter**: Only applicable if using API Gateway pattern.
- **In-process rate limiting (Guava RateLimiter)**: Doesn't share state across instances.

## Consequences

- Redis must be available for rate limiting to function (graceful degradation is handled by the circuit breaker).
- IP-based limiting may be too aggressive behind shared proxies; consider user-based limiting in the future.
