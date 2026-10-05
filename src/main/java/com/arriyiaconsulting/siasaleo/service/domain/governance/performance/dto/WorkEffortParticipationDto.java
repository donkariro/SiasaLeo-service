package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record WorkEffortParticipationDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("work_effort_id") Long workEffortId,
    @JsonbProperty("party_id") Long partyId,
    @JsonbProperty("role_code") String roleCode,
    @JsonbProperty("officeholder_id") Long officeholderId,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("date_precision") DatePrecision datePrecision,
    @JsonbProperty("asset_version_id") Long assetVersionId,
    @JsonbProperty("locator") String locator,
    @JsonbProperty("recorded_by_account_id") Long recordedByAccountId,
    @JsonbProperty("created_at") OffsetDateTime createdAt
) {}
