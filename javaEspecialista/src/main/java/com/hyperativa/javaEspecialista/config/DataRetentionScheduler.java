package com.hyperativa.javaEspecialista.config;

import com.hyperativa.javaEspecialista.domain.ports.out.CardRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for data retention policy enforcement.
 * Deletes cards whose {@code expires_at} timestamp has passed.
 * Runs daily at 02:00 AM by default (configurable).
 */
@Component
@EnableScheduling
public class DataRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionScheduler.class);
    private final CardRepositoryPort cardRepository;

    public DataRetentionScheduler(CardRepositoryPort cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Scheduled(cron = "${app.data-retention.cleanup-cron:0 0 2 * * ?}")
    public void cleanupExpiredCards() {
        log.info("Data retention cleanup started");
        int deleted = cardRepository.deleteExpiredCards();
        log.info("Data retention cleanup completed: {} cards removed", deleted);
    }
}
