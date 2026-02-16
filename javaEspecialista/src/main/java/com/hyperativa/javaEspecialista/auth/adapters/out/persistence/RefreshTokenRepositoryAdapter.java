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
        repository.revokeAllByUserId(userId.toString());
    }

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(
                UUID.fromString(entity.getId()),
                entity.getToken(),
                UUID.fromString(entity.getUserId()),
                entity.getExpiryDate(),
                entity.isRevoked());
    }

    private RefreshTokenEntity toEntity(RefreshToken domain) {
        String id;
        boolean isNew = false;
        if (domain.id() == null) {
            id = UUID.randomUUID().toString();
            isNew = true;
        } else {
            id = domain.id().toString();
        }

        RefreshTokenEntity entity = new RefreshTokenEntity(
                id,
                domain.token(),
                domain.userId().toString(),
                domain.expiryDate(),
                domain.revoked());
        entity.setNew(isNew);
        return entity;
    }
}
