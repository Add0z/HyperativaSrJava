package com.hyperativa.javaEspecialista.auth.domain.model;

public record TokenPair(String accessToken, String refreshToken, String tokenType, long expiresIn) {
}
