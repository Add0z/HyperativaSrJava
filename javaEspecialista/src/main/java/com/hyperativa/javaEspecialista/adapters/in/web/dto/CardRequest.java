package com.hyperativa.javaEspecialista.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CardRequest(
        @NotBlank(message = "Card number is required") @Pattern(regexp = "^\\d+$", message = "Card number must contain only digits") String cardNumber) {
}
