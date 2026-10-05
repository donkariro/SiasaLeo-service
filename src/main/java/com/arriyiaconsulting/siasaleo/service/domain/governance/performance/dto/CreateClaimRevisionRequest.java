package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateClaimRevisionRequest(
    @JsonbProperty("claim_id") @Positive Long claimId,
    @JsonbProperty("officeholder_id") @NotNull @Positive Long officeholderId,
    @JsonbProperty("title") @NotBlank @Size(max = 255) String title,
    @JsonbProperty("description") @NotBlank String description,
    @JsonbProperty("claimant_party_id") @Positive Long claimantPartyId,
    @JsonbProperty("work_effort_id") @Positive Long workEffortId,
    @JsonbProperty("scope_id") @Positive Long scopeId,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("date_precision") DatePrecision datePrecision,
    @JsonbProperty("source_reference") @Size(max = 2048) String sourceReference
) {}
