package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record ObservationDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("indicator_id") Long indicatorId,
    @JsonbProperty("actual_value") BigDecimal actualValue,
    @JsonbProperty("qualitative_result") String qualitativeResult,
    @JsonbProperty("officeholder_id") Long officeholderId,
    @JsonbProperty("work_effort_id") Long workEffortId,
    @JsonbProperty("scope_id") Long scopeId,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("date_precision") DatePrecision datePrecision,
    @JsonbProperty("source_reference") String sourceReference,
    @JsonbProperty("recorded_by_account_id") Long recordedByAccountId,
    @JsonbProperty("recorded_at") OffsetDateTime recordedAt,
    @JsonbProperty("publication_status") PublicationStatus publicationStatus,
    @JsonbProperty("published_at") OffsetDateTime publishedAt,
    @JsonbProperty("legacy_measurement_id") Long legacyMeasurementId,
    @JsonbProperty("row_version") Long rowVersion
) {}
