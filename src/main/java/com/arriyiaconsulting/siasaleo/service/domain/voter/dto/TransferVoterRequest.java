package com.arriyiaconsulting.siasaleo.service.domain.voter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Moves a voter's active registration to another centre. The person comes from
 * the request path; registrationDate dates the new registration and defaults
 * to today.
 */
public record TransferVoterRequest(
        @NotNull Long registrationCenterId,
        @PastOrPresent LocalDate registrationDate) {
}
