package com.arriyiaconsulting.siasaleo.service.domain.candidate.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record CandidacyDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long personId,
        @Schema(required = true) String firstName,
        @Schema(required = true) String lastName,
        @Schema(required = true) Long contestId,
        // Null for independent candidates; still listed as required so
        // generated client types get `number | null` rather than an optional
        // property (same for the name and abbreviation).
        @Schema(required = true, nullable = true) Long politicalPartyId,
        @Schema(required = true, nullable = true) String partyName,
        @Schema(required = true, nullable = true) String partyAbbreviation,
        @Schema(required = true) String status) {
}
