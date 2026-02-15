package com.hyperativa.javaEspecialista.auth.domain.port.in;

import com.hyperativa.javaEspecialista.auth.domain.model.TokenPair;

public interface AuthInputPort {
    TokenPair login(String username, String password);

    TokenPair refreshToken(String refreshToken);

    void register(String username, String password,
            java.util.Set<com.hyperativa.javaEspecialista.auth.domain.model.Role> roles);
}
