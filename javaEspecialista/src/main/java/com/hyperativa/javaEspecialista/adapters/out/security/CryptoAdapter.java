package com.hyperativa.javaEspecialista.adapters.out.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hyperativa.javaEspecialista.domain.ports.out.CryptoPort;
import com.hyperativa.javaEspecialista.domain.ports.out.MetricsPort;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Component
public class CryptoAdapter implements CryptoPort {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_TAG_LENGTH = 128; // in bits
    private static final int IV_LENGTH = 12; // 12 bytes for GCM

    private final SecretKey encryptionKey;
    private final SecretKey hashKey;
    private final MetricsPort metricsService;

    public CryptoAdapter(@Value("${app.security.encryption-key}") String encryptionKeyStr,
            @Value("${app.security.hash-key}") String hashKeyStr,
            MetricsPort metricsService) {
        // Assuming keys are provided as Base64 encoded strings
        this.encryptionKey = new SecretKeySpec(Base64.getDecoder().decode(encryptionKeyStr), "AES");
        this.hashKey = new SecretKeySpec(Base64.getDecoder().decode(hashKeyStr), HMAC_ALGORITHM);
        this.metricsService = metricsService;
    }

    @Override
    public byte[] encrypt(String plainText, byte[] iv) {
        Instant start = Instant.now();
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, parameterSpec);
            byte[] result = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            metricsService.recordCryptoEncryptLatency(Duration.between(start, Instant.now()));
            return result;
        } catch (Exception e) {
            metricsService.incrementCryptoFailure();
            throw new com.hyperativa.javaEspecialista.domain.exception.EncryptionException("Error during encryption",
                    e);
        }
    }

    @Override
    public byte[] hash(String plainText) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hashKey);
            return mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            metricsService.incrementCryptoFailure();
            throw new com.hyperativa.javaEspecialista.domain.exception.EncryptionException("Error during hashing", e);
        }
    }

    @Override
    public String decrypt(byte[] cipherText, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, parameterSpec);
            byte[] result = cipher.doFinal(cipherText);
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            metricsService.incrementCryptoFailure();
            throw new com.hyperativa.javaEspecialista.domain.exception.EncryptionException("Error during decryption",
                    e);
        }
    }

    @Override
    public byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
