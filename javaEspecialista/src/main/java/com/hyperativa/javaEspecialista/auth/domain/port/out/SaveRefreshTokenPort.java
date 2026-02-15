package com.hyperativa.javaEspecialista.auth.domain.port.out;

import com.hyperativa.javaEspecialista.auth.domain.model.RefreshToken;
import java.util.UUID;

public interface SaveRefreshTokenPort {
    RefreshToken save(RefreshToken refreshToken);

    void revokeAllUserTokens(UUID userId);
}
