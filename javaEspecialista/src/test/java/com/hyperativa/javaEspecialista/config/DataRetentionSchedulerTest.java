package com.hyperativa.javaEspecialista.config;

import com.hyperativa.javaEspecialista.domain.ports.out.CardRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataRetentionSchedulerTest {

    @Mock
    private CardRepositoryPort cardRepository;

    @InjectMocks
    private DataRetentionScheduler scheduler;

    @Test
    void cleanupExpiredCards_ShouldCallDeleteExpiredCards() {
        // Given
        when(cardRepository.deleteExpiredCards()).thenReturn(5);

        // When
        scheduler.cleanupExpiredCards();

        // Then
        verify(cardRepository).deleteExpiredCards();
    }
}
