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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Batch file adapter that processes card registrations using Java 25 virtual
 * threads
 * for maximum throughput. Each card registration is submitted to a virtual
 * thread,
 * enabling high concurrency without platform thread exhaustion.
 */
@Component
public class BatchFileAdapter {

    private static final Logger log = LoggerFactory.getLogger(BatchFileAdapter.class);
    private final CardInputPort cardInputPort;

    public BatchFileAdapter(CardInputPort cardInputPort) {
        this.cardInputPort = cardInputPort;
    }

    public BatchResponse processFile(MultipartFile file) {
        ParseResult parsed = parseFile(file);
        List<CardLine> cardLines = parsed.cards;

        int total = parsed.totalLines;
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);
        List<BatchError> errors = Collections.synchronizedList(new ArrayList<>());

        // Process with virtual threads for maximum concurrency
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (CardLine cardLine : cardLines) {
                futures.add(executor.submit(() -> {
                    try {
                        if (cardLine.cardNumber().isEmpty()) {
                            errors.add(new BatchError(cardLine.lineNumber(), "****", "Empty card number"));
                            log.warn("Empty card number at line: {}", cardLine.lineNumber());
                            failure.incrementAndGet();
                            return;
                        }
                        cardInputPort.registerCard(cardLine.cardNumber());
                        success.incrementAndGet();
                    } catch (Exception e) {
                        String maskedCard = maskCardNumber(cardLine.cardNumber());
                        String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                        errors.add(new BatchError(cardLine.lineNumber(), maskedCard, reason));
                        log.error("Failed to process line: {} - Card: {} - Reason: {}",
                                cardLine.lineNumber(), maskedCard, reason, e);
                        failure.incrementAndGet();
                    }
                }));
            }

            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("Unexpected error waiting for virtual thread", e);
                }
            }
        }

        return new BatchResponse(total, success.get(), failure.get(), errors);
    }

    /**
     * Parse the batch file and extract card numbers. This is done sequentially
     * since file I/O is inherently sequential. The parsed cards are then
     * processed in parallel via virtual threads.
     */
    private ParseResult parseFile(MultipartFile file) {
        List<CardLine> cards = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip Header
                if (line.startsWith("DESAFIO"))
                    continue;
                // Skip Footer or empty lines
                if (line.trim().isEmpty() || line.startsWith("LOTE") || line.length() < 26)
                    continue;

                try {
                    String rawCardNumber = line.substring(7, 26).trim();
                    cards.add(new CardLine(lineNumber, rawCardNumber));
                } catch (Exception e) {
                    cards.add(new CardLine(lineNumber, ""));
                }
            }
        } catch (IOException e) {
            log.error("Error reading file", e);
            throw new com.hyperativa.javaEspecialista.domain.exception.FileProcessingException("Failed to process file",
                    e);
        }

        log.info("Parsed {} card lines from batch file ({} total lines), processing with virtual threads",
                cards.size(), lineNumber);
        return new ParseResult(cards, lineNumber);
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    private record CardLine(int lineNumber, String cardNumber) {
    }

    private record ParseResult(List<CardLine> cards, int totalLines) {
    }
}
