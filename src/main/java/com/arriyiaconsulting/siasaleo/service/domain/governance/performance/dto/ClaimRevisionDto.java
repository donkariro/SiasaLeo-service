package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record ClaimRevisionDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("officeholder_id") Long officeholderId,
    @JsonbProperty("title") String title,
    @JsonbProperty("description") String description,
    @JsonbProperty("period_start") LocalDate periodStart,
    @JsonbProperty("period_end") LocalDate periodEnd,
    @JsonbProperty("source_reference") String sourceReference,
    @JsonbProperty("created_at") OffsetDateTime createdAt,
    @JsonbProperty("claim_id") Long claimId,
    @JsonbProperty("revision_number") Integer revisionNumber,
    @JsonbProperty("claimant_party_id") Long claimantPartyId,
    @JsonbProperty("recorded_by_account_id") Long recordedByAccountId,
    @JsonbProperty("work_effort_id") Long workEffortId,
    @JsonbProperty("date_precision") DatePrecision datePrecision,
    @JsonbProperty("publication_status") PublicationStatus publicationStatus,
    @JsonbProperty("published_at") OffsetDateTime publishedAt,
    @JsonbProperty("scope_id") Long scopeId,
    @JsonbProperty("row_version") Long rowVersion
) {}
