package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record AssessmentDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("record_id") Long recordId,
    @JsonbProperty("commitment_revision_id") Long commitmentRevisionId,
    @JsonbProperty("commitment_target_id") Long commitmentTargetId,
    @JsonbProperty("observation_id") Long observationId,
    @JsonbProperty("delivery_status") DeliveryStatus deliveryStatus,
    @JsonbProperty("evidence_status") EvidenceStatus evidenceStatus,
    @JsonbProperty("criteria") String criteria,
    @JsonbProperty("rationale") String rationale,
    @JsonbProperty("reviewed_by_account_id") Long reviewedByAccountId,
    @JsonbProperty("assessed_at") OffsetDateTime assessedAt,
    @JsonbProperty("publication_status") PublicationStatus publicationStatus,
    @JsonbProperty("published_at") OffsetDateTime publishedAt,
    @JsonbProperty("row_version") Long rowVersion
) {}
