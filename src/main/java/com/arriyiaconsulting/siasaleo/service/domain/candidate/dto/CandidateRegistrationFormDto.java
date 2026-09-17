package com.arriyiaconsulting.siasaleo.service.domain.candidate.dto;

import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ProfileDetails;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegistrationDto;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Load before rendering the form; locked fields are also protected on submission. */
public record CandidateRegistrationFormDto(
        @Schema(required = true) boolean registeredVoter,
        @Schema(required = true) boolean voterFieldsReadOnly,
        @Schema(required = true, nullable = true) ProfileDetails profile,
        @Schema(required = true, nullable = true) VoterRegistrationDto voterRegistration) {
}
