package com.hyperativa.javaEspecialista.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response for batch file processing")
public record BatchResponse(
                @Schema(description = "Total number of lines processed in the file", example = "17") int totalLinesProcessed,

                @Schema(description = "Number of cards successfully registered", example = "14") int successCount,

                @Schema(description = "Number of cards that failed to register", example = "1") int failureCount,

                @Schema(description = "Detailed information about each failure") List<BatchError> errors) {
}
