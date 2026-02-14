package com.hyperativa.javaEspecialista.domain.ports.out;

public interface CryptoPort {
    byte[] encrypt(String plainText, byte[] iv);

    /**
     * Decrypts ciphertext using AES-256-GCM.
     *
     * @param cipherText the encrypted data (ciphertext + GCM tag concatenated)
     * @param iv         the initialization vector used during encryption
     * @return the decrypted plaintext string
     */
    String decrypt(byte[] cipherText, byte[] iv);

    byte[] hash(String plainText);

    byte[] generateIv();
}
