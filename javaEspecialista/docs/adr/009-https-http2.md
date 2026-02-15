# ADR-009: HTTPS + HTTP/2

## Status

Accepted

## Context

The API handles credit card numbers and authentication credentials. All data in transit must be encrypted. Additionally, the API serves multiple concurrent clients that benefit from HTTP/2's multiplexing capabilities.

## Decision

The application is exposed **exclusively over HTTPS** with **HTTP/2 enabled** and HTTP/1.1 as fallback.

### Implementation Details

- **TLS**: PKCS12 keystore (`keystore.p12`) configured in `application.yml`.
- **Port**: `8443` (HTTPS only, no HTTP listener).
- **HTTP/2**: Enabled via `server.http2.enabled: true`.
- **Certificate**: Self-signed for development; must be replaced with a CA-signed certificate in production.

### Configuration (application.yml)

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD:password}
    key-store-type: PKCS12
    key-alias: javaespecialista
  http2:
    enabled: true
```

## Rationale

- **PCI DSS Req 4**: Encrypt transmission of cardholder data across open, public networks.
- **LGPD**: Personal data must be protected during transmission.
- **HTTP/2 benefits**: Stream multiplexing (multiple requests over a single connection), header compression (HPACK), and server push capability reduce latency for concurrent API consumers.
- **Fallback**: HTTP/1.1 is automatically negotiated for clients that don't support HTTP/2 (ALPN negotiation).

## Alternatives Considered

- **TLS termination at reverse proxy (Nginx/HAProxy)**: Valid in production but adds infrastructure complexity for the challenge scope. The application self-terminates TLS for simplicity.
- **HTTP only**: Unacceptable for financial data transmission.

## Consequences

- All clients must use `https://` URLs.
- Self-signed certificates require `-k` flag in cURL or trust store configuration in clients.
- Production deployments should use CA-signed certificates and potentially move TLS termination to a reverse proxy.
