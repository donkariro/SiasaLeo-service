package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A political party as the ORPP register describes it. Symbol, colours and
 * slogan are left out for now: V20 gave each its own validity period, so they
 * belong in sub-resources rather than as flat fields here.
 * <p>
 * Only the name is guaranteed present; the rest are blank in the register for
 * some parties, and are listed as required-but-nullable so generated client
 * types get `string | null` rather than an optional property.
 */
public record PoliticalPartyDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true, nullable = true) String abbreviation,
        @Schema(required = true, nullable = true) String registrationNumber,
        @Schema(required = true, nullable = true) LocalDate registeredOn,
        @Schema(required = true, nullable = true) String postalAddress,
        @Schema(required = true, nullable = true) String headOfficeLocation,
        @Schema(required = true, nullable = true) String changes) {

    public static PoliticalPartyDto from(PoliticalParty party) {
        return new PoliticalPartyDto(party.getId(), party.getName(),
                party.getAbbreviation(), party.getRegistrationNumber(),
                party.getRegisteredOn(), party.getPostalAddress(),
                party.getHeadOfficeLocation(), party.getChanges());
    }
}
