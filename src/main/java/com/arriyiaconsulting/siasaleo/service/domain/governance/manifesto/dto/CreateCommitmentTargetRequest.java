package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateCommitmentTargetRequest(
    @JsonbProperty("indicator_id") @NotNull @Positive Long indicatorId,
    @JsonbProperty("scope_id") @NotNull @Positive Long scopeId,
    @JsonbProperty("baseline_value") BigDecimal baselineValue,
    @JsonbProperty("target_value") @NotNull BigDecimal targetValue,
    @JsonbProperty("period_start") @NotNull LocalDate periodStart,
    @JsonbProperty("period_end") @NotNull LocalDate periodEnd
) {}
