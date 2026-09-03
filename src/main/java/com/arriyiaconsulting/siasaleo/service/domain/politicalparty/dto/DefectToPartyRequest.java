package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Moves a member to another party. The person comes from the request path;
 * startDate is the first day of the new membership and defaults to today. The
 * membership being left is closed the day before, so startDate must fall after
 * the day that membership began.
 */
public record DefectToPartyRequest(
        @NotNull Long politicalPartyId,
        @PastOrPresent LocalDate startDate) {
}
