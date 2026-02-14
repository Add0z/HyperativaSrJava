package com.hyperativa.javaEspecialista.domain.ports.out;

public interface CryptoPort {
    byte[] encrypt(String plainText, byte[] iv);

    byte[] hash(String plainText);

    byte[] generateIv();
}
