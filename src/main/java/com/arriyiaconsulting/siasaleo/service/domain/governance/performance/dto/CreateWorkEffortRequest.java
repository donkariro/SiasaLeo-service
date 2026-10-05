package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateWorkEffortRequest(
    @JsonbProperty("title") @NotBlank @Size(max = 255) String title,
    @JsonbProperty("description") @NotBlank String description,
    @JsonbProperty("type_code") @NotBlank @Size(max = 40) String typeCode,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("date_precision") DatePrecision datePrecision,
    @JsonbProperty("scope_id") @Positive Long scopeId
) {}
