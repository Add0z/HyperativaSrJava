# 5. Audit Logging Strategy

Date: 2026-02-15

## Status

Accepted

## Context

The application processes sensitive credit card data (PCI DSS) and personal data (LGPD). Compliance regulations require a robust audit trail for all access and modifications to this data. Specifically, PCI DSS Requirement 10 mandates tracking "who did what, where, and when" for all access to cardholder data.

## Decision

We will implement a synchronous audit logging mechanism using a dedicated `AuditPort` interface and a persistence adapter (`AuditRepositoryAdapter`).

### Why Synchronous?

We chose synchronous logging over asynchronous (e.g., Kafka, `@Async`) for the following reasons, driven by the strict compliance nature of this project:

1. **Consistency & Non-Repudiation**: Synchronous logging guarantees that if the system confirms an action (e.g., "HTTP 200 OK"), the audit trail is **already** persisted. This prevents scenarios where a business operation succeeds but the audit log is lost due to an immediate crash, ensuring a complete and reliable audit trail.
2. **Transactional Integrity**: Audit logs for business transactions should be bound to the success of that transaction.
3. **Simplicity**: given the current throughput requirements, the millisecond-level latency overhead is a worthy trade-off for the guarantee of data integrity without the complexity of distributed tracing or potential message loss in async queues.

### Future Improvements

* **Async with Kafka**: If the application scales to a point where synchronous DB writes become a bottleneck, we will refactor to use **Kafka** as a durable log. This will require implementing the **Transactional Outbox Pattern** to solve the "Dual Write" problem and ensure consistency between the business transaction and the audit event.

### Key Implementation Details

1. **Identity Capture**:
    * **User ID**: Extracted from the security context (JWT token) via `SecurityPort`.
    * **IP Address**: Extracted from the HTTP request (`X-Forwarded-For` or remote address) via `SecurityPort`.
    * **Role**: The "system" user is used only for internal background processes; all API interactions must be attributable to a specific user or service account.

2. **Scope**:
    * **Card Operations**: Registration, Lookup, Deletion, Batch Upload.
    * **Authentication Events**: Login (Success/Failure), Registration (Success/Failure).

3. **Data Structure**:
    * `timestamp`: When the event occurred.
    * `user_id`: The principal (username) performing the action.
    * `action`: A categorical identifier (e.g., `CARD_LOOKUP`, `LOGIN_FAILURE`).
    * `resource_id`: The ID of the affected resource (e.g., card UUID, username), if applicable.
    * `ip_address`: The source IP of the request.
    * `result`: `SUCCESS`, `FAILURE`, or `PENDING`.
    * `details`: Additional context (e.g., specific error messages).

4. **Storage**:
    * Audit logs are stored in a relational database table (`audit_logs`) for durability and queryability.
    * Sensitive data (PANs) are **never** logged, complying with PCI DSS.

## Consequences

### Positive

* **Compliance**: Meets PCI DSS Requirement 10 and LGPD accountability requirements.
* **Traceability**: Enables forensic analysis of security incidents.
* **Accountability**: Every action is linked to a specific user and IP address.

### Negative

* **Performance Overhead**: Synchronous logging adds a small latency to each request. This is an acceptable trade-off for strict consistency and compliance guarantees.
* **Storage Growth**: The `audit_logs` table will grow indefinitely. A data retention/archival strategy (e.g., move to cold storage after 1 year) will be needed in the future.
