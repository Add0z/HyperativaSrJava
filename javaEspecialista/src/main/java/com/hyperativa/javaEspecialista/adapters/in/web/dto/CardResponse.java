package com.hyperativa.javaEspecialista.adapters.in.web.dto;

import java.util.UUID;

/**
 * Response containing the PCI DSS token (non-reversible substitute for the
 * PAN).
 */
public record CardResponse(UUID token) {
}
