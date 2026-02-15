package com.hyperativa.javaEspecialista.auth.domain.port.out;

import com.hyperativa.javaEspecialista.auth.domain.model.User;
import java.util.Optional;

public interface LoadUserPort {
    Optional<User> loadUserByUsername(String username);

    Optional<User> loadUserById(java.util.UUID id);
}
