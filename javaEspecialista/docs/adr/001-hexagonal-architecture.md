# ADR-001: Hexagonal Architecture

## Status

Accepted

## Context

The challenge requires building a card registration and lookup API with security, persistence, and observability concerns. The architecture must support testability, framework independence, and clean separation of concerns.

## Decision

We adopted **Hexagonal Architecture (Ports & Adapters)** over the traditional layered MVC pattern.

### Structure

```
domain/
  model/        → Pure domain entities (Card, User)
  ports/in/     → Input ports (CardInputPort, AuthInputPort)
  ports/out/    → Output ports (CardRepositoryPort, CryptoPort, AuditPort)
  service/      → Domain services (CardService, MetricsService)
adapters/
  in/web/       → REST controllers
  in/file/      → Batch file processing
  out/persistence/ → Spring Data JDBC repositories
  out/security/    → AES-256-GCM encryption
config/         → Spring wiring (DomainConfig, SecurityConfig)
```

## Rationale

- **Domain isolation**: `CardService` has zero Spring annotations — it's a plain Java class instantiated via `DomainConfig`. This makes domain logic fully testable with Mockito alone.
- **Framework swapability**: Swapping from Spring Data JDBC to JPA or R2DBC only requires changing the `out/persistence` adapter.
- **Testability**: Unit tests mock ports, not Spring beans. Integration tests verify adapter wiring.

## Alternatives Considered

- **Layered MVC**: Simpler but couples business logic to Spring annotations. Domain would depend on `@Service`, `@Transactional`, etc.
- **Clean Architecture**: More layers than needed for this project size.

## Consequences

- Extra configuration class (`DomainConfig`) to wire domain services.
- Port interfaces add indirection, but enforces contracts between layers.
