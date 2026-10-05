package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CommitmentTargetDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("commitment_revision_id") Long commitmentRevisionId,
    @JsonbProperty("indicator_id") Long indicatorId,
    @JsonbProperty("scope_id") Long scopeId,
    @JsonbProperty("baseline_value") BigDecimal baselineValue,
    @JsonbProperty("target_value") BigDecimal targetValue,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd
) {}
