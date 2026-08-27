package com.arriyiaconsulting.siasaleo.service.domain.candidate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Registers a candidate for a contest. The candidate is either an existing
 * person (personId) or one created on the fly (person) — exactly one of the
 * two must be given. politicalPartyId is null for independent candidates.
 */
public record RegisterCandidateRequest(
        Long personId,
        @Valid NewPerson person,
        @NotNull Long contestId,
        Long politicalPartyId) {

    public record NewPerson(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Past LocalDate dateOfBirth) {
    }
}
