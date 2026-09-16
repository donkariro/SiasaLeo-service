package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** A political party option for candidate registration and other selection controls. */
public record PoliticalPartyOptionDto(
        @Schema(required = true) Long id,
        @Schema(required = true) String name,
        @Schema(required = true, nullable = true) String abbreviation) {

    public static PoliticalPartyOptionDto from(PoliticalParty party) {
        return new PoliticalPartyOptionDto(party.getId(), party.getName(), party.getAbbreviation());
    }
}
