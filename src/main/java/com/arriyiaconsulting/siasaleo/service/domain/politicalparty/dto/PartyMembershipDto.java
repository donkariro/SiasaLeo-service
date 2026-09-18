package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record PartyMembershipDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long personId,
        @Schema(required = true) String firstName,
        @Schema(required = true) String lastName,
        @Schema(required = true) Long politicalPartyId,
        @Schema(required = true) String partyName,
        // Blank in the ORPP register for a few parties; still listed as
        // required so generated client types get `string | null` rather than
        // an optional property (same for endDate).
        @Schema(required = true, nullable = true) String partyAbbreviation,
        @Schema(required = true) LocalDate startDate,
        // Null while the membership is current.
        @Schema(required = true, nullable = true) LocalDate endDate,
        @Schema(required = true) boolean current) {
}
