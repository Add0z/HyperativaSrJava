# 10. Adopt Refresh Token Rotation

Date: 2024-02-15

## Status

Accepted

## Context

The current authentication mechanism issues a single JWT Access Token with a relatively long expiration (e.g., 1 hour) upon login. This approach has significant security drawbacks:

1. **Revocation Difficulty**: Stateless JWTs cannot be easily revoked before expiration without maintaining a blacklist, which impacts performance.
2. **Security Risk**: If an access token is stolen, an attacker has a wide window of opportunity (up to 1 hour) to act on behalf of the user.
3. **User Experience vs. Security Trade-off**: Reducing the expiration time improves security but forces users to log in more frequently, degrading UX.

To meet "Big Corp" / Fintech security standards, we need a mechanism that allows for short-lived access tokens (minimizing the attack window) while maintaining a seamless user experience (via long-lived sessions) and enabling immediate revocation.

## Decision

We will implement a **Refresh Token Rotation** strategy.

1. **Dual Token System**:
    * **Access Token**: Short-lived (e.g., 15 minutes). Used for API access.
    * **Refresh Token**: Long-lived (e.g., 7 days). Used *only* to obtain a new Access Token.

2. **Rotation Policy**:
    * Every time a Refresh Token is used to get a new Access Token, the **Refresh Token itself is also rotated** (a new one is issued, and the old one is invalidated).
    * This ensures that if a Refresh Token is stolen and used by an attacker, the legitimate user's next attempt to use their (now old) Refresh Token will fail.

3. **Reuse Detection**:
    * If an invalidated (already used) Refresh Token is presented, the system will assume a breach and **invalidate the entire token family** (all tokens associated with that user/session), forcing a re-login.

## Consequences

### Positive

* **Enhanced Security**: Access tokens are short-lived, minimizing damage from theft.
* **Immediate Revocation**: Admins or the system can revoke a user's Refresh Token in the database, effectively killing their session when the short-lived access token expires.
* **Breach Detection**: Token rotation allows detection of stolen tokens via reuse attempts.

### Negative

* **Complexity**: Increases implementation complexity (requires database storage for refresh tokens, rotation logic, and handling race conditions).
* **Database Load**: Every token refresh requires a database write (to update/rotate the token).

## Alternatives Considered

* **Slide Expiration**: Just issuing a new Access Token without a Refresh Token. Rejected because it doesn't solve the revocation problem for the active token.
* **Reference Tokens**: Storing opaque tokens in the DB and validating on every request. Rejected because it couples every API request to the database, impacting performance compared to stateless JWT validation.
