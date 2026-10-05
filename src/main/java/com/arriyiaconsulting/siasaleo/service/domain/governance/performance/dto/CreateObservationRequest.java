package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateObservationRequest(
    @JsonbProperty("indicator_id") @Positive Long indicatorId,
    @JsonbProperty("actual_value") BigDecimal actualValue,
    @JsonbProperty("qualitative_result") String qualitativeResult,
    @JsonbProperty("officeholder_id") @Positive Long officeholderId,
    @JsonbProperty("work_effort_id") @Positive Long workEffortId,
    @JsonbProperty("scope_id") @Positive Long scopeId,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("date_precision") DatePrecision datePrecision,
    @JsonbProperty("source_reference") @Size(max = 2048) String sourceReference
) {}
