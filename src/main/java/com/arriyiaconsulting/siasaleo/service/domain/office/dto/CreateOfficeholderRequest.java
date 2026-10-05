package com.arriyiaconsulting.siasaleo.service.domain.office.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateOfficeholderRequest(
    @JsonbProperty("seat_id") @NotNull @Positive Long seatId,
    @JsonbProperty("person_id") @NotNull @Positive Long personId,
    @JsonbProperty("term_id") @NotNull @Positive Long termId,
    @JsonbProperty("candidacy_id") @Positive Long candidacyId,
    @JsonbProperty("entry_reason_id") @NotNull @Positive Long entryReasonId,
    @JsonbProperty("exit_reason_id") @Positive Long exitReasonId,
    @JsonbProperty("start_date") @NotNull LocalDate startDate,
    @JsonbProperty("end_date") LocalDate endDate,
    @JsonbProperty("asset_version_id") @NotNull @Positive Long assetVersionId,
    @Size(max = 255) String locator
) {}
