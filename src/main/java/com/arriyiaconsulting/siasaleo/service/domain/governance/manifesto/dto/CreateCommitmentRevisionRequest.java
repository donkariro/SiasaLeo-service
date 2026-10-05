package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateCommitmentRevisionRequest(
    @JsonbProperty("wording") @NotBlank String wording,
    @JsonbProperty("conditions") String conditions,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("date_precision") DatePrecision datePrecision,
    @JsonbProperty("scope_id") @Positive Long scopeId
) {}
