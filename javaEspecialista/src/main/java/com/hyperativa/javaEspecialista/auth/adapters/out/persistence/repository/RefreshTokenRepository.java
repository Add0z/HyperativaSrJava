package com.hyperativa.javaEspecialista.auth.adapters.out.persistence.repository;

import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE refresh_tokens SET revoked = true WHERE user_id = :userId")
    void revokeAllByUserId(UUID userId);

    List<RefreshTokenEntity> findAllByUserId(UUID userId);
}
