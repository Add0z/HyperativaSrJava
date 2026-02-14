package com.hyperativa.javaEspecialista.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details of a batch processing error")
public record BatchError(
        @Schema(description = "Line number in the file where the error occurred", example = "5") int lineNumber,

        @Schema(description = "Card number that failed validation", example = "5105105105105100") String cardNumber,

        @Schema(description = "Reason for the failure", example = "Invalid card number (Luhn check failed)") String reason) {
}
