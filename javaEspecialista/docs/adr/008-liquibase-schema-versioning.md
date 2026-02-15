# ADR-008: Liquibase for Schema Versioning

## Status

Accepted

## Context

The application requires a MySQL database with tables for cards, users, roles, and audit logs. Schema changes must be version-controlled, reproducible, and safe to apply across environments (development, staging, production).

## Decision

We use **Liquibase** to manage all database schema migrations via YAML changelog files.

### Implementation Details

- **Master changelog**: `db/changelog/db.changelog-master.yaml`
- **Changesets**:
  - `001-create-auth-tables.yaml` — Users, roles, and user-roles tables
  - `002-create-card-tables.yaml` — Cards table with encrypted columns
  - `003-create-audit-log-table.yaml` — Audit log table
  - `004-add-expires-at-column.yaml` — Data retention expiration column
- **Format**: YAML (consistent with `application.yml` convention)
- **Execution**: Automatic on application startup (`spring.liquibase.enabled: true`)

## Rationale

- **Traceability**: Every schema change is versioned, attributed, and auditable.
- **Reproducibility**: Any environment can be rebuilt from scratch by running all changesets.
- **Safety**: Checksums prevent silent modification of already-applied changesets.
- **Rollback**: Liquibase supports rollback strategies for failed migrations.

## Alternatives Considered

- **Flyway**: Comparable functionality but uses SQL files by default; Liquibase's YAML format was preferred for consistency.
- **JPA auto-DDL (`ddl-auto: update`)**: Unsafe for production — no rollback, no traceability, and can silently drop data.
- **Manual SQL scripts**: No checksums, no execution tracking, error-prone.

## Consequences

- All schema changes must go through Liquibase changesets; direct DDL modifications are prohibited.
- The `schema.sql` file is kept for reference but is not used at runtime.
