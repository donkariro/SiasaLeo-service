package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Enrols a person in a political party. The person must not already belong to
 * one — defecting is a separate operation. startDate is optional and defaults
 * to today, so recording an enrolment as it happens needs no date; supplying
 * one is for backfilling historical records.
 */
public record JoinPartyRequest(
        @NotNull Long personId,
        @NotNull Long politicalPartyId,
        @PastOrPresent LocalDate startDate) {
}
