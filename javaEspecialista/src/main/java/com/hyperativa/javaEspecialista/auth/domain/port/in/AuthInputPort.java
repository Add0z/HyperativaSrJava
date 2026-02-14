package com.hyperativa.javaEspecialista.auth.domain.port.in;

public interface AuthInputPort {
    String login(String username, String password);

    void register(String username, String password);
}
