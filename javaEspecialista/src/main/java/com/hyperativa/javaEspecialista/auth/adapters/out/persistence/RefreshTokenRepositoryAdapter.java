package com.hyperativa.javaEspecialista.auth.adapters.out.persistence;

import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.entity.RefreshTokenEntity;
import com.hyperativa.javaEspecialista.auth.adapters.out.persistence.repository.RefreshTokenRepository;
import com.hyperativa.javaEspecialista.auth.domain.model.RefreshToken;
import com.hyperativa.javaEspecialista.auth.domain.port.out.LoadRefreshTokenPort;
import com.hyperativa.javaEspecialista.auth.domain.port.out.SaveRefreshTokenPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenRepositoryAdapter implements LoadRefreshTokenPort, SaveRefreshTokenPort {

    private final RefreshTokenRepository repository;

    public RefreshTokenRepositoryAdapter(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RefreshToken> loadByToken(String token) {
        return repository.findByToken(token)
                .map(this::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = toEntity(refreshToken);
        RefreshTokenEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void revokeAllUserTokens(UUID userId) {
        repository.revokeAllByUserId(userId);
    }

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(
                entity.getId(),
                entity.getToken(),
                entity.getUserId(),
                entity.getExpiryDate(),
                entity.isRevoked());
    }

    private RefreshTokenEntity toEntity(RefreshToken domain) {
        UUID id = domain.id();
        boolean isNew = false;
        if (id == null) {
            id = UUID.randomUUID();
            isNew = true;
        }

        RefreshTokenEntity entity = new RefreshTokenEntity(
                id,
                domain.token(),
                domain.userId(),
                domain.expiryDate(),
                domain.revoked());
        entity.setNew(isNew);
        return entity;
    }
}
