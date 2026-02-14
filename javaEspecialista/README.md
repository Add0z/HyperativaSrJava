# Secure Card Registry — Java Specialist Challenge (SrJava)

Project developed as a technical challenge for the Backend Java Specialist position at Hyperativa.
The goal is to build a high-security REST API for credit card tokenization, with PCI DSS and LGPD compliance, focused on performance, resilience, and clean architecture.

## 📋 Table of Contents

- [🚀 Getting Started](#-getting-started)
  - [Service Access](#service-access)
- [🔐 Authentication Flow](#-authentication-flow)
  - [1. Register a User](#1-register-a-user)
  - [2. Login and Get Token](#2-login-and-get-token)
  - [3. Use the Token](#3-use-the-token)
  - [Password Requirements](#password-requirements)
- [📍 Main Endpoints](#-main-endpoints)
  - [Functional](#functional)
    - [1. Register Card (Tokenize)](#1-register-card-tokenize)
    - [2. Lookup Token by Card Number](#2-lookup-token-by-card-number)
    - [3. Batch Upload (File)](#3-batch-upload-file)
    - [4. Erase Card Data (LGPD)](#4-erase-card-data-lgpd)
  - [Observability & Actuator](#observability--actuator)
- [🏗 Architecture Decision Records (ADR)](#-architecture-decision-records-adr)
  - [1. Hexagonal Architecture](#1-hexagonal-architecture-ports--adapters)
  - [2. AES-256-GCM + HMAC-SHA-256 Encryption](#2-aes-256-gcm--hmac-sha-256-encryption)
  - [3. Redis Cache with TTL](#3-redis-cache-with-ttl)
  - [4. Distributed Rate Limiting](#4-distributed-rate-limiting)
  - [5. Circuit Breaker (Resilience4j)](#5-circuit-breaker-resilience4j)
- [🔑 Key Rotation](#-key-rotation)
- [⚙️ Environment Variables](#️-environment-variables)
- [🧪 Testing](#-testing)
- [🛡 Security Compliance](#-security-compliance)
- [📬 Postman](#-postman)

## 🚀 Getting Started

To bring up the full stack (Application, MySQL, Redis, Prometheus, Grafana):

```bash
docker compose -f compose-full.yaml up --build
```

For development mode, start `JavaEspecialistaApplication.java` from your IDE. The project uses Spring Boot Docker Compose, which is pre-configured to start only the required dependencies (MySQL, Redis, Prometheus, Grafana, Adminer):

```bash
docker compose up --build
```

Or alternatively, via Maven:

```bash
mvn spring-boot:run
```

### Service Access

| Service | URL | Credentials (User/Pass) |
|---------|-----|-------------------------|
| **Application (HTTPS)** | `https://localhost:8443` | JWT Bearer Token |
| **Swagger UI** | `https://localhost:8443/swagger-ui.html` | - |
| **OpenAPI JSON** | `https://localhost:8443/v3/api-docs` | - |
| **Prometheus** | `http://localhost:9090` | - |
| **Grafana** | `http://localhost:3000` | `admin` / `admin` |
| **Adminer (MySQL Web)** | `http://localhost:8081` | Server: `mysql`, User: `user`, Pass: `password`, DB: `card_registry` |
| **MySQL** | `localhost:3306` | `user` / `password` (DB: `card_registry`) |
| **Redis** | `localhost:6379` | - |

## 🔐 Authentication Flow

All endpoints (except `/api/v1/auth/**`) require a JWT Bearer Token.

### 1. Register a User

```bash
curl -k -X POST https://localhost:8443/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "password": "StrongPass1!xy"}'
```

**Example Response (201 Created)**:

```json
{
  "id": 1,
  "username": "user1"
}
```

### 2. Login and Get Token

```bash
TOKEN=$(curl -sk -X POST https://localhost:8443/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "password": "StrongPass1!xy"}' | jq -r '.token')
```

**Example Response (200 OK)**:

```json
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 3. Use the Token

```bash
curl -k -X POST https://localhost:8443/api/v1/cards \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cardNumber": "4539578763621486"}'
```

### Password Requirements

- Minimum 12 characters
- At least one uppercase, one lowercase, one digit, and one special character (`@$!%*?&`)

## 📍 Main Endpoints

### Functional

#### 1. Register Card (Tokenize)

- **POST** `/api/v1/cards`
- **Auth**: JWT Bearer Token
- **Description**: Receives the card number, validates it (Luhn), encrypts it (AES-256-GCM), generates a hash (HMAC-SHA-256), and returns a unique token.
- **Status**: `201 Created`
- **Example Call (cURL)**:

```bash
curl -k -X POST https://localhost:8443/api/v1/cards \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cardNumber": "4539578763621486"}'
```

**Example Response**:

```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

#### 2. Lookup Token by Card Number

- **POST** `/api/v1/cards/lookup`
- **Auth**: JWT Bearer Token
- **Description**: Receives the card number and returns the associated token, if it exists.
- **Status**: `200 OK`
- **Example Call (cURL)**:

```bash
curl -k -X POST https://localhost:8443/api/v1/cards/lookup \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cardNumber": "4539578763621486"}'
```

**Example Response**:

```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

#### 3. Batch Upload (File)

- **POST** `/api/v1/cards/upload`
- **Auth**: JWT Bearer Token
- **Description**: Receives a text file with multiple card numbers (positional format) and processes them in batch.
- **Status**: `200 OK`
- **Example Call (cURL)**:

```bash
curl -k -X POST https://localhost:8443/api/v1/cards/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@postman/test-cards.txt"
```

**Example Response**:

```json
{
  "totalProcessed": 10,
  "success": 8,
  "failed": 2
}
```

#### 4. Erase Card Data (LGPD)

- **POST** `/api/v1/cards/delete`
- **Auth**: JWT Bearer Token
- **Description**: Removes card data in compliance with LGPD (Art. 18 — Right to Erasure).
- **Status**: `204 No Content`
- **Example Call (cURL)**:

```bash
curl -k -X POST https://localhost:8443/api/v1/cards/delete \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cardNumber": "4539578763621486"}'
```

### Observability & Actuator

The application exposes metrics and health checks on port `8443` (HTTPS).

| Endpoint | Description |
|----------|-------------|
| **GET** `/actuator/health` | Application and dependency health status (MySQL, Redis). |
| **GET** `/actuator/prometheus` | Metrics in Prometheus format for scraping. |
| **GET** `/actuator/metrics` | Lists available metrics (JVM, HTTP, HikariCP). |
| **GET** `/actuator` | Lists all available management endpoints. |

```bash
curl -k https://localhost:8443/actuator/health
```

```bash
curl -k https://localhost:8443/actuator/prometheus
```

#### Custom Business & Security Metrics

| Metric | Type | Description | Tags |
|--------|------|-------------|------|
| `cards_registered_total` | Counter | Total cards successfully registered. | - |
| `cards_already_exists_total` | Counter | Total attempts to register already existing cards. | - |
| `cards_validation_failed_total` | Counter | Total card validation failures (Luhn). | - |
| `card_lookup_total` | Counter | Total card lookups. | - |
| `card_lookup_found_total` | Counter | Total lookups where card was found. | - |
| `card_lookup_not_found_total` | Counter | Total lookups where card was not found. | - |
| `cache_hits_total` | Counter | Total cache hits (Redis). | - |
| `cache_misses_total` | Counter | Total cache misses (Redis). | - |
| `cache_get_latency_seconds` | Timer | Latency of cache GET operations. | - |
| `cache_put_latency_seconds` | Timer | Latency of cache PUT operations. | - |
| `crypto_encrypt_latency_seconds` | Timer | Latency of encryption operations. | - |
| `crypto_failures_total` | Counter | Total cryptographic failures. | - |
| `rate_limit_allowed_total` | Counter | Total requests allowed by the rate limiter. | `client_id`, `endpoint` |
| `rate_limit_blocked_total` | Counter | Total requests blocked by the rate limiter. | `client_id`, `endpoint` |
| `auth_login_success_total` | Counter | Total successful logins. | - |
| `auth_login_failure_total` | Counter | Total failed logins. | `reason` |
| `auth_user_registered_total` | Counter | Total users successfully registered. | - |
| `auth_user_registered_by_role_total` | Counter | Total users registered by role. | `role` |
| `auth_user_registration_failed_total` | Counter | Total failed user registrations. | `reason` |

## 🏗 Architecture Decision Records (ADR)

### 1. Hexagonal Architecture (Ports & Adapters)

**Decision**: Isolate the domain from infrastructure.
**Motivation**: Ensure that business rules (`domain`) have zero dependencies on frameworks or external libraries (no Spring imports in the domain layer).
**Structure**:

```
src/main/java/com/hyperativa/javaEspecialista/
├── domain/                  # Pure business logic (zero framework imports)
│   ├── model/               # Card, value objects (immutable Records)
│   ├── service/             # CardService
│   ├── ports/in/            # CardInputPort
│   ├── ports/out/           # CardRepositoryPort, CryptoPort, MetricsPort, AuditPort
│   └── exception/           # Domain exceptions
├── auth/domain/             # Auth domain (also framework-free)
│   ├── model/               # User, Role
│   ├── service/             # AuthService
│   ├── port/in/             # AuthInputPort
│   ├── port/out/            # LoadUserPort, SaveUserPort, PasswordEncoderPort
│   └── exception/           # AuthenticationException
├── adapters/in/web/         # REST controllers, exception handler
├── adapters/in/file/        # Batch file processing
├── adapters/out/persistence/# MySQL repositories (Spring Data JDBC)
├── adapters/out/security/   # Crypto (AES/HMAC), JWT, password encoding
├── adapters/out/metrics/    # Micrometer metrics adapter
└── config/                  # Spring configuration, Security, HikariCP
```

### 2. AES-256-GCM + HMAC-SHA-256 Encryption

**Decision**: Strong encryption with guaranteed integrity for card data.
**Motivation**: PCI DSS compliance requires that PANs are never stored in plaintext. The combination of AES-256-GCM (confidentiality + authentication) with HMAC-SHA-256 (deterministic lookup without exposing the PAN) provides the ideal balance.
**Impact**: The PAN is never stored in plaintext; lookups are performed via hash, and decryption occurs only when strictly necessary.

### 3. Redis Cache with TTL

**Decision**: Distributed cache (Redis) for frequently looked-up card tokens.
**Motivation**: Reduce latency of repeated MySQL queries and decrease database load.
**Configuration**:

- **Library**: Spring Data Redis (Lettuce driver).
- **Policies**:
  - Configurable TTL via `application.yml`.
  - Automatic invalidation on expiration.
- **Flow**: The adapter attempts to retrieve from Redis cache; on cache miss, it queries MySQL and populates the cache.

### 4. Distributed Rate Limiting

**Decision**: Redis-based rate limiting for request control per IP/user.
**Motivation**: Protect the API against abuse and DDoS attacks, distributing control across multiple application instances.
**Impact**: Requests exceeding the limit receive `429 Too Many Requests`.

### 5. Circuit Breaker (Resilience4j)

**Decision**: Circuit Breaker on the persistence adapter.
**Motivation**: Prevent failure cascading when MySQL or Redis are unavailable, releasing resources quickly and allowing graceful recovery.
**Configuration**: Parameterized via `application.yml` with failure thresholds and half-open timeout.

### 6. Liquibase for Schema Versioning

**Decision**: Use Liquibase to manage database migrations.
**Motivation**: Ensure traceability, reproducibility, and safety across all schema changes, from development to production.

### 7. HTTPS + HTTP/2

**Decision**: API exposed exclusively over HTTPS with HTTP/2 enabled.
**Motivation**: Security requirement for sensitive financial data. HTTP/2 provides stream multiplexing and header compression, improving performance.
**Fallback**: HTTP/1.1 only as a fallback for incompatible clients.

## 🔑 Key Rotation

### Encryption Key Rotation (AES-256-GCM)

1. Generate a new 32-byte key: `openssl rand -base64 32`
2. Re-encrypt existing records (decrypt with old key, encrypt with new key).
3. Update the `ENCRYPTION_KEY` environment variable.
4. Restart the application.

### JWT Key Rotation (RSA)

1. Generate a new key pair: `openssl genrsa -out new-private.pem 2048` and `openssl rsa -pubout -in new-private.pem -out new-public.pem`
2. Update `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY`.
3. Restart the application; existing tokens will expire naturally.

### HMAC Key Rotation (HMAC-SHA-256)

⚠️ Rotating the `HASH_KEY` requires re-hashing all existing records, as lookups depend on hash comparison. Plan for a migration window.

## ⚙️ Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MYSQL_HOST` | MySQL host | `localhost` |
| `MYSQL_PORT` | MySQL port | `3306` |
| `MYSQL_DATABASE` | Database name | `card_registry` |
| `MYSQL_USER` | MySQL user | `root` |
| `MYSQL_PASSWORD` | MySQL password | `secret` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `ENCRYPTION_KEY` | AES-256 key (Base64, 32 bytes) | Dev key in `application.yml` |
| `HASH_KEY` | HMAC-SHA-256 key (Base64, 32 bytes) | Dev key in `application.yml` |
| `JWT_PUBLIC_KEY` | RSA public key for JWT verification | `classpath:public.pem` |
| `JWT_PRIVATE_KEY` | RSA private key for JWT signing | `classpath:private.pem` |
| `TOKEN_ISSUER` | JWT issuer | `hyperativa` |
| `TOKEN_AUDIENCE` | JWT audience | `card-api` |

> **⚠️ IMPORTANT**: In production, generate strong random keys and provide them via environment variables. Never use the default development keys.

## 🧪 Testing

```bash
mvn test                # Run all 72+ unit tests
mvn verify              # Run tests + integration tests
```

## 🛡 Security Compliance

- **PCI DSS**: Audit trail, card tokenization, AES-256-GCM encryption, no PAN in logs.
- **LGPD**: Data erasure endpoint (`POST /api/v1/cards/delete`), audit logging.
- **OWASP**: Security headers (HSTS, CSP, X-Frame-Options), CSRF protection, rate limiting.

## 📬 Postman

The Postman collection and test files are available in the `postman/` folder.

**How to Use:**

1. Import the collection `postman/Hyperativa.postman_collection.json`.
2. Set the `baseUrl` environment variable to `https://localhost:8443`.
3. Run the **Register** request followed by **Login** to obtain a token.
4. The token will be automatically used in all subsequent requests.
5. For batch upload, use the file `postman/test-cards.txt`.
