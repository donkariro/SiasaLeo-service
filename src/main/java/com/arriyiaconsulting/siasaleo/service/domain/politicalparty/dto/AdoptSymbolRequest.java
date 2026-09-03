package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Puts a new symbol on the party's ballot line. A party presents exactly one
 * symbol at a time, so this closes the symbol in use the day before fromDate.
 * fromDate is optional and defaults to today; supplying one is for backfilling
 * historical records.
 */
public record AdoptSymbolRequest(
        @NotBlank @Size(max = 255) String symbolDescription,
        @Size(max = 255) String imageFile,
        @PastOrPresent LocalDate fromDate) {
}
