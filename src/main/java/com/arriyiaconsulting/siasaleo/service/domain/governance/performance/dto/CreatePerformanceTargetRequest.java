package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreatePerformanceTargetRequest(
    @JsonbProperty("officeholder_id") @NotNull @Positive Long officeholderId,
    @JsonbProperty("indicator_id") @NotNull @Positive Long indicatorId,
    @JsonbProperty("scope_id") @Positive Long scopeId,
    @JsonbProperty("period_start") @NotNull LocalDate periodStart,
    @JsonbProperty("period_end") @NotNull LocalDate periodEnd,
    @JsonbProperty("baseline_value") BigDecimal baselineValue,
    @JsonbProperty("target_value") @NotNull BigDecimal targetValue,
    @JsonbProperty("source_reference") @NotBlank @Size(max = 2048) String sourceReference,
    @JsonbProperty("notes") String notes
) {}
