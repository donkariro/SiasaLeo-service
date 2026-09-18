package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record PoliticalPartySymbolDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long politicalPartyId,
        @Schema(required = true) String symbolDescription,
        @Schema(required = true, nullable = true) String imageFile,
        // Null where the register never recorded when the symbol came in.
        @Schema(required = true, nullable = true) LocalDate fromDate,
        // Null while the symbol is the one on the ballot.
        @Schema(required = true, nullable = true) LocalDate uptoDate,
        @Schema(required = true) boolean current) {
}
