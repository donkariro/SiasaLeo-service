package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Adds a slogan to the party. Slogans carry no unique index, so this leaves
 * any others in use alone. fromDate is optional and defaults to today.
 */
public record AdoptSloganRequest(
        @NotBlank @Size(max = 255) String slogan,
        @PastOrPresent LocalDate fromDate) {
}
