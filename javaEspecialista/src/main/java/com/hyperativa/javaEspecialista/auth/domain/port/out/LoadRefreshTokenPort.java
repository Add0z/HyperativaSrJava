package com.hyperativa.javaEspecialista.auth.domain.port.out;

import com.hyperativa.javaEspecialista.auth.domain.model.RefreshToken;
import java.util.Optional;

public interface LoadRefreshTokenPort {
    Optional<RefreshToken> loadByToken(String token);
}
