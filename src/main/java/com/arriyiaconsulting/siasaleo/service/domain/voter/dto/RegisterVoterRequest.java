package com.arriyiaconsulting.siasaleo.service.domain.voter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Registers a person to vote at a centre. registrationCenterId must be an
 * electoral area of type REGISTRATION_CENTER. registrationDate is optional and
 * defaults to today, so recording a registration as it happens needs no date;
 * supplying one is for backfilling historical records.
 */
public record RegisterVoterRequest(
        @NotNull Long personId,
        @NotNull Long registrationCenterId,
        @PastOrPresent LocalDate registrationDate) {
}
