package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Takes a slogan out of use. uptoDate is the last day it applied and defaults
 * to today, so the whole body is optional.
 */
public record RetireSloganRequest(
        @PastOrPresent LocalDate uptoDate) {
}
