package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateWorkEffortParticipationRequest(
    @JsonbProperty("work_effort_id") @NotNull @Positive Long workEffortId,
    @JsonbProperty("party_id") @NotNull @Positive Long partyId,
    @JsonbProperty("role_code") @NotBlank @Size(max = 40) String roleCode,
    @JsonbProperty("officeholder_id") @Positive Long officeholderId,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("date_precision") DatePrecision datePrecision,
    @JsonbProperty("asset_version_id") @NotNull @Positive Long assetVersionId,
    @JsonbProperty("locator") @Size(max = 255) String locator
) {}
