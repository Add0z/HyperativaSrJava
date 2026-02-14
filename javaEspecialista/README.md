# Secure Card Registry - Java Specialist Challenge

High-performance, secure REST API for credit card registration and lookup, compliant with strict security and resilience requirements.

## Technnical Stack

- **Java 25** + **Spring Boot 4.0.2**
- **Hexagonal Architecture** (Ports & Adapters)
- **Security**: AES-256-GCM Encryption, HMAC-SHA-256 Hashing
- **Persistence**: MySQL 8.0 (Spring Data JDBC) + Redis (Cache)
- **Resilience**: Rate Limiting (Redis), Circuit Breaker (Resilience4j)
- **Observability**: Prometheus + Grafana + Structured Logs

## Quick Start

### 1. Build and Run Full Stack

The provided `docker-compose.yml` orchestrates the application and all infrastructure services.

```bash
docker-compose up --build
```

This will start:

- App: <http://localhost:8080>
- MySQL: 3306
- Redis: 6379
- Prometheus: 9090
- Grafana: 3000

### 2. Access Documentation

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Json**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### 3. Usage Examples

**Register Card (Batch)**
Upload `docs/DESAFIO-HYPERATIVA.txt`:

```bash
curl -F "file=@docs/DESAFIO-HYPERATIVA.txt" http://localhost:8080/api/v1/cards/upload
```

**Get Card UUID**

```bash
curl http://localhost:8080/api/v1/cards/{valid-card-number}
```

## Security

Encryption keys are configured via environment variables.

- `ENCRYPTION_KEY`: Base64 encoded 32-byte key.
- `HASH_KEY`: Base64 encoded 32-byte key.
See `docker-compose.yml` for defaults used in development.
