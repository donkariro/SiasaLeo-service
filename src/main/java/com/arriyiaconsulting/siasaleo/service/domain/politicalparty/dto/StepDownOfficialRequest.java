package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Ends a tenure in a party office. uptoDate is the last day the person held
 * the position and defaults to today, so the whole body is optional.
 */
public record StepDownOfficialRequest(
        @PastOrPresent LocalDate uptoDate) {
}
