package com.hyperativa.javaEspecialista.auth.adapters.out.persistence.repo;

import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.UserEntity;

public interface UserRepository extends CrudRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);
}
