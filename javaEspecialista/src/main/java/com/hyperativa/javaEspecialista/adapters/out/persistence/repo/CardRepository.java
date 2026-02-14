package com.hyperativa.javaEspecialista.adapters.out.persistence.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.hyperativa.javaEspecialista.adapters.out.persistence.entity.CardEntity;

import java.util.Optional;

@Repository
public interface CardRepository extends CrudRepository<CardEntity, Long> {
    @org.springframework.data.jdbc.repository.query.Query("SELECT * FROM cards WHERE card_hash = :cardHash")
    Optional<CardEntity> findByCardHash(byte[] cardHash);

    Optional<CardEntity> findByUuid(String uuid);

    @org.springframework.data.jdbc.repository.query.Modifying
    @org.springframework.data.jdbc.repository.query.Query("DELETE FROM cards WHERE card_hash = :cardHash")
    int deleteByCardHash(byte[] cardHash);

    @org.springframework.data.jdbc.repository.query.Modifying
    @org.springframework.data.jdbc.repository.query.Query("DELETE FROM cards WHERE expires_at IS NOT NULL AND expires_at < NOW()")
    int deleteExpired();
}
