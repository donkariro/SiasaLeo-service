package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CommitmentRevisionDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("commitment_id") Long commitmentId,
    @JsonbProperty("revision_number") Integer revisionNumber,
    @JsonbProperty("wording") String wording,
    @JsonbProperty("conditions") String conditions,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("date_precision") DatePrecision datePrecision,
    @JsonbProperty("scope_id") Long scopeId,
    @JsonbProperty("recorded_by_account_id") Long recordedByAccountId,
    @JsonbProperty("created_at") OffsetDateTime createdAt,
    @JsonbProperty("sealed_at") OffsetDateTime sealedAt,
    @JsonbProperty("row_version") Long rowVersion
) {}
