package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Appoints a person to an office in a political party. positionName is free
 * text — the schema keeps no register of positions — and is matched exactly,
 * after trimming, when checking whether the person already holds it.
 * fromDate is optional and defaults to today, so recording an appointment as
 * it happens needs no date; supplying one is for backfilling historical
 * records.
 */
public record AppointOfficialRequest(
        @NotNull Long officialId,
        @NotNull Long politicalPartyId,
        @NotBlank @Size(max = 100) String positionName,
        @PastOrPresent LocalDate fromDate,
        @Size(max = 255) String photo,
        String about) {
}
