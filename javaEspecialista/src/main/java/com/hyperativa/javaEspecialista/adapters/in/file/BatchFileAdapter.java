package com.hyperativa.javaEspecialista.adapters.in.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.hyperativa.javaEspecialista.adapters.in.web.dto.BatchError;
import com.hyperativa.javaEspecialista.adapters.in.web.dto.BatchResponse;
import com.hyperativa.javaEspecialista.domain.ports.in.CardInputPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class BatchFileAdapter {

    private static final Logger log = LoggerFactory.getLogger(BatchFileAdapter.class);
    private final CardInputPort cardInputPort;

    public BatchFileAdapter(CardInputPort cardInputPort) {
        this.cardInputPort = cardInputPort;
    }

    public BatchResponse processFile(MultipartFile file) {
        int total = 0;
        int success = 0;
        int failure = 0;
        List<BatchError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                // Skip Header
                if (line.startsWith("DESAFIO")) {
                    continue;
                }
                // Skip Footer or empty lines
                if (line.trim().isEmpty() || line.startsWith("LOTE") || line.length() < 26) {
                    continue;
                }

                try {
                    // Extract Card Number: [08-26] (1-based) -> [7-26] (0-based exclusive)
                    // The requirement says [08-26], consisting of 19 characters.
                    // Java substring(beginIndex, endIndex)
                    // beginIndex = 7 (8th char)
                    // endIndex = 26 (27th char) => length 26-7 = 19.
                    String rawCardNumber = line.substring(7, 26).trim();

                    if (rawCardNumber.isEmpty()) {
                        failure++;
                        errors.add(new BatchError(total, "****", "Empty card number"));
                        log.warn("Empty card number at line: {}", total);
                        continue;
                    }

                    // Register
                    cardInputPort.registerCard(rawCardNumber);
                    success++;

                } catch (Exception e) {
                    String maskedCard = "****";
                    try {
                        String extracted = line.substring(7, 26).trim();
                        maskedCard = maskCardNumber(extracted);
                    } catch (Exception ignored) {
                        // If we can't extract the card number, leave it masked
                    }

                    String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    errors.add(new BatchError(total, maskedCard, reason));
                    log.error("Failed to process line: {} - Card: {} - Reason: {}", total, maskedCard, reason, e);
                    failure++;
                }
            }

        } catch (IOException e) {
            log.error("Error reading file", e);
            throw new com.hyperativa.javaEspecialista.domain.exception.FileProcessingException("Failed to process file",
                    e);
        }

        return new BatchResponse(total, success, failure, errors);
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
}
