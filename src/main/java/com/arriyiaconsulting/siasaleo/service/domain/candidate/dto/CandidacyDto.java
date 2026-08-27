package com.arriyiaconsulting.siasaleo.service.domain.candidate.dto;

import com.arriyiaconsulting.siasaleo.service.domain.candidate.entity.Candidacy;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record CandidacyDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long personId,
        @Schema(required = true) String firstName,
        @Schema(required = true) String lastName,
        @Schema(required = true) Long contestId,
        // Null for independent candidates; still listed as required so
        // generated client types get `number | null` rather than an optional
        // property (same for the abbreviation).
        @Schema(required = true, nullable = true) Long politicalPartyId,
        @Schema(required = true, nullable = true) String partyAbbreviation,
        @Schema(required = true) String status) {

    public static CandidacyDto from(Candidacy candidacy) {
        Person person = candidacy.getPerson();
        PoliticalParty party = candidacy.getPoliticalParty();
        return new CandidacyDto(candidacy.getId(), person.getId(), person.getFirstName(),
                person.getLastName(), candidacy.getContestId(),
                party != null ? party.getId() : null,
                party != null ? party.getAbbreviation() : null,
                candidacy.getStatus().getStatusName());
    }
}
