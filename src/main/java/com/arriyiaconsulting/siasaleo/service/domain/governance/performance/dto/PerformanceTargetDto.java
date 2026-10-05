package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record PerformanceTargetDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("officeholder_id") Long officeholderId,
    @JsonbProperty("indicator_id") Long indicatorId,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("baseline_value") BigDecimal baselineValue,
    @JsonbProperty("target_value") BigDecimal targetValue,
    @JsonbProperty("source_reference") String sourceReference,
    @JsonbProperty("notes") String notes,
    @JsonbProperty("created_at") OffsetDateTime createdAt,
    @JsonbProperty("scope_id") Long scopeId,
    @JsonbProperty("row_version") Long rowVersion
) {}
