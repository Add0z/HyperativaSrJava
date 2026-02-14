# ADR-003: HMAC-SHA-256 for Card Lookup

## Status

Accepted

## Context

We need to look up cards by number without decrypting all stored records. Searching encrypted data is not feasible with AES-GCM since each encryption produces different ciphertext (due to unique IVs).

## Decision

We compute an **HMAC-SHA-256 hash** of each card number and use it as a deterministic lookup key.

## Rationale

- **Deterministic**: Same input always produces the same hash, enabling exact-match queries via database index.
- **Keyed hash**: Unlike SHA-256, HMAC requires a secret key (`HASH_KEY`), preventing rainbow table attacks even if the database is compromised.
- **One-way**: The card number cannot be recovered from the hash, satisfying PCI DSS requirements for non-reversible hashing.

### Query Flow

```
1. Client sends PAN → API
2. API computes HMAC-SHA-256(PAN, HASH_KEY) → hash
3. API queries: SELECT uuid FROM cards WHERE card_hash = ?
4. Returns token (UUID) or 404
```

This avoids scanning and decrypting all records for every lookup.

## Alternatives Considered

- **Plain SHA-256**: Vulnerable to rainbow table attacks since PAN space is finite (~10^16).
- **bcrypt/scrypt**: Too slow for lookup operations (intentionally designed to be slow).
- **Searchable encryption (SSE)**: Adds significant complexity without matching benefit for exact-match queries.

## Consequences

- `HASH_KEY` must differ from `ENCRYPTION_KEY` (defense in depth).
- If `HASH_KEY` is rotated, all hashes must be recomputed.
