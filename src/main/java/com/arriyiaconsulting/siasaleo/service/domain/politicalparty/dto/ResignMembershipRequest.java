package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Ends a member's party membership without a replacement. endDate is the last
 * day of the membership and defaults to today, so the whole body is optional.
 */
public record ResignMembershipRequest(
        @PastOrPresent LocalDate endDate) {
}
