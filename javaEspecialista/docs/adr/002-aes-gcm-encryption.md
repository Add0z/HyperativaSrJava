# ADR-002: AES-256-GCM Encryption for Card Data

## Status

Accepted

## Context

Credit card numbers (PANs) must be stored securely in the database. PCI DSS Requirement 3 mandates encryption of stored cardholder data with strong cryptography.

## Decision

We use **AES-256-GCM** (Galois/Counter Mode) for encrypting PANs at rest.

## Rationale

- **Authenticated encryption**: GCM provides both confidentiality and integrity (authentication tag), unlike AES-CBC which only provides confidentiality and requires separate HMAC for integrity.
- **Performance**: GCM is parallelizable and hardware-accelerated on modern CPUs via AES-NI.
- **Industry standard**: Recommended by NIST SP 800-38D and accepted by PCI DSS.

### Implementation Details

- **Key**: 256-bit, injected via environment variable (`ENCRYPTION_KEY`)
- **IV**: 12 bytes, randomly generated per encryption (never reused)
- **Tag**: 128-bit authentication tag, stored alongside ciphertext
- **Storage**: `encrypted_card` (ciphertext), `encryption_iv`, `encryption_tag` stored in separate columns

## Alternatives Considered

- **AES-CBC + HMAC-SHA-256**: More complex (two operations), no hardware acceleration for HMAC, and requires careful MAC-then-Encrypt/Encrypt-then-MAC ordering.
- **ChaCha20-Poly1305**: Good alternative but less supported in Java's standard library.
- **RSA**: Asymmetric encryption adds unnecessary key management complexity for data-at-rest.

## Consequences

- IV must never be reused with the same key (enforced by `SecureRandom`).
- Key rotation requires re-encrypting existing data (see ADR-004).
