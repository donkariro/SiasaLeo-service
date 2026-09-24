package com.arriyiaconsulting.siasaleo.service.domain.candidate.dto;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ProfileDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record ImportCandidacyRequest(@NotNull @Positive Long contestId, @Positive Long personId,
        @Valid ProfileDetails profile, @Positive Long politicalPartyId, @NotNull @Positive Long statusId,
        @NotBlank @Size(max=255) String ballotName, @Size(max=255) String ballotPartyName,
        @NotBlank @Size(max=2048) String sourceReference, @NotBlank @Size(max=1024) String sourceRecordReference) { }
