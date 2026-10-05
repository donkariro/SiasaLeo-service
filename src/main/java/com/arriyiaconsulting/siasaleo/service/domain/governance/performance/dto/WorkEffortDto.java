package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record WorkEffortDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("title") String title,
    @JsonbProperty("description") String description,
    @JsonbProperty("type_code") String typeCode,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("date_precision") DatePrecision datePrecision,
    @JsonbProperty("scope_id") Long scopeId,
    @JsonbProperty("recorded_by_account_id") Long recordedByAccountId,
    @JsonbProperty("created_at") OffsetDateTime createdAt,
    @JsonbProperty("row_version") Long rowVersion
) {}
