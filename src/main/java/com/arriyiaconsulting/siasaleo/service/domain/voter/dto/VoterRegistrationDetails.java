package com.arriyiaconsulting.siasaleo.service.domain.voter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** Voter details for a person already resolved by the candidate registration flow. */
public record VoterRegistrationDetails(
        @NotNull @Positive Long registrationCenterId,
        @PastOrPresent LocalDate registrationDate) {
}
