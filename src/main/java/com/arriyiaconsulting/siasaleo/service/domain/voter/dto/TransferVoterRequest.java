package com.arriyiaconsulting.siasaleo.service.domain.voter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Moves the caller's active registration to another centre. Like
 * RegisterVoterRequest, the voter is the authenticated account's person and is
 * not named here. registrationDate dates the new registration and defaults to
 * today.
 */
public record TransferVoterRequest(
        @NotNull Long registrationCenterId,
        @PastOrPresent LocalDate registrationDate) {
}
