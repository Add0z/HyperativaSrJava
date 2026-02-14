package com.hyperativa.javaEspecialista.auth.domain.port.in;

public interface AuthInputPort {
    String login(String username, String password);

    void register(String username, String password,
            java.util.Set<com.hyperativa.javaEspecialista.auth.domain.model.Role> roles);
}
