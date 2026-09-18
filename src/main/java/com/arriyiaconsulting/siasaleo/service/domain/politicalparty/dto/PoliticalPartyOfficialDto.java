package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A tenure in a party office. Everything the register may leave blank is
 * listed as required-but-nullable so generated client types get `T | null`
 * rather than an optional property.
 */
public record PoliticalPartyOfficialDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long officialId,
        @Schema(required = true) String firstName,
        @Schema(required = true) String lastName,
        @Schema(required = true) Long politicalPartyId,
        @Schema(required = true) String partyName,
        @Schema(required = true, nullable = true) String partyAbbreviation,
        @Schema(required = true) String positionName,
        // Null where the register never recorded when the tenure began.
        @Schema(required = true, nullable = true) LocalDate fromDate,
        // Null while the person still holds the position.
        @Schema(required = true, nullable = true) LocalDate uptoDate,
        @Schema(required = true, nullable = true) String photo,
        @Schema(required = true, nullable = true) String about,
        @Schema(required = true) boolean current) {
}
