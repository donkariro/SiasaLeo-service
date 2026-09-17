package com.arriyiaconsulting.siasaleo.service.domain.candidate.dto;

import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ProfileDetails;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Self-registration: the person is resolved from the authenticated account. */
public record RegisterCandidateRequest(
        @Valid ProfileDetails profile,
        @NotNull @Positive Long contestId,
        @Positive Long politicalPartyId,
        @Valid VoterRegistrationDetails voterRegistration) {
}
