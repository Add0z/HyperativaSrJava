package com.hyperativa.javaEspecialista.adapters.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hyperativa.javaEspecialista.adapters.in.file.BatchFileAdapter;
import com.hyperativa.javaEspecialista.adapters.in.web.dto.BatchResponse;
import com.hyperativa.javaEspecialista.adapters.in.web.dto.CardRequest;
import com.hyperativa.javaEspecialista.adapters.in.web.dto.CardResponse;
import com.hyperativa.javaEspecialista.domain.ports.in.CardInputPort;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@Tag(name = "Cards", description = "Card Management APIs")
@PreAuthorize("isAuthenticated()")
public class CardController {

    private static final Logger log = LoggerFactory.getLogger(CardController.class);

    private final CardInputPort cardInputPort;
    private final BatchFileAdapter batchFileAdapter;

    public CardController(CardInputPort cardInputPort,
            BatchFileAdapter batchFileAdapter) {
        this.cardInputPort = cardInputPort;
        this.batchFileAdapter = batchFileAdapter;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a new card", description = "Registers a card. If it exists, returns the existing UUID.")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<CardResponse> registerCard(@Valid @RequestBody CardRequest request,
            java.security.Principal principal) {
        log.info("User {} registered a card: {}", principal.getName(), maskCardNumber(request.cardNumber()));
        UUID uuid = cardInputPort.registerCard(request.cardNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CardResponse(uuid));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload batch file", description = "Uploads a TXT file containing card records for batch registration.")
    public ResponseEntity<BatchResponse> uploadFile(
            @RequestParam("file") MultipartFile file, java.security.Principal principal) {
        log.info("User {} requested to upload file: {}", principal.getName(), file.getOriginalFilename());
        return ResponseEntity.ok(batchFileAdapter.processFile(file));
    }

    @PostMapping("/search")
    @Operation(summary = "Get card UUID (Secure)", description = "Retrieves the UUID of a registered card by its number via POST to avoid URL logging.")
    public ResponseEntity<CardResponse> getCardSecure(@Valid @RequestBody CardRequest request,
            java.security.Principal principal) {
        log.info("User {} requested to get card (secure) by number: {}", principal.getName(),
                maskCardNumber(request.cardNumber()));
        return cardInputPort.findCardUuid(request.cardNumber())
                .map(uuid -> ResponseEntity.ok(new CardResponse(uuid)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Operation(summary = "Delete card (LGPD)", description = "Permanently erases card data per LGPD Art. 18 (right to erasure). Card number sent via body for security.")
    public ResponseEntity<Void> deleteCard(@Valid @RequestBody CardRequest request,
            java.security.Principal principal) {
        log.info("User {} requested card deletion (LGPD Art. 18): {}",
                principal.getName(), maskCardNumber(request.cardNumber()));
        boolean deleted = cardInputPort.deleteCard(request.cardNumber());
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "************" + cardNumber.substring(cardNumber.length() - 4);
    }

}
