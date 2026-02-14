package com.hyperativa.javaEspecialista.adapters.out.security;

import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CryptoAdapterTest {

    @Mock
    private MetricsPort metricsService;

    private CryptoAdapter cryptoAdapter;

    // Use valid Base64 for 32 bytes (256 bits) for AES
    private final String encryptionKey = Base64.getEncoder().encodeToString(new byte[32]);
    private final String hashKey = Base64.getEncoder().encodeToString(new byte[32]);

    @BeforeEach
    void setUp() {
        cryptoAdapter = new CryptoAdapter(encryptionKey, hashKey, metricsService);
    }

    @Test
    void encrypt_ShouldReturnEncryptedData() {
        String plainText = "test-card-number";
        byte[] iv = cryptoAdapter.generateIv();

        byte[] encrypted = cryptoAdapter.encrypt(plainText, iv);

        assertNotNull(encrypted);
        assertTrue(encrypted.length > 0);
        verify(metricsService).recordCryptoEncryptLatency(any());
    }

    @Test
    void hash_ShouldReturnHmac() {
        String plainText = "test-card-number";

        byte[] hash = cryptoAdapter.hash(plainText);

        assertNotNull(hash);
        assertEquals(32, hash.length); // SHA-256 HMAC is 32 bytes
    }

    @Test
    void generateIv_ShouldReturnValidIv() {
        byte[] iv = cryptoAdapter.generateIv();
        assertNotNull(iv);
        assertEquals(12, iv.length); // GCM IV length
    }

    @Test
    void encrypt_WhenErrorOccurs_ShouldIncrementFailureMetric() {
        // Passing null plainText to trigger exception
        assertThrows(RuntimeException.class, () -> cryptoAdapter.encrypt(null, cryptoAdapter.generateIv()));
        verify(metricsService).incrementCryptoFailure();
    }
}
