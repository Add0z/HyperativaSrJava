package com.hyperativa.javaEspecialista.auth.domain.port.out;

import com.hyperativa.javaEspecialista.auth.domain.model.User;

public interface SaveUserPort {
    void save(User user);
}
