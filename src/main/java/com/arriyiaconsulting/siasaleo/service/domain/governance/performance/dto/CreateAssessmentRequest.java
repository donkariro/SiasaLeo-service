package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateAssessmentRequest(
    @JsonbProperty("record_id") @Positive Long recordId,
    @JsonbProperty("commitment_revision_id") @Positive Long commitmentRevisionId,
    @JsonbProperty("commitment_target_id") @Positive Long commitmentTargetId,
    @JsonbProperty("observation_id") @Positive Long observationId,
    @JsonbProperty("delivery_status") DeliveryStatus deliveryStatus,
    @JsonbProperty("evidence_status") @NotNull EvidenceStatus evidenceStatus,
    @JsonbProperty("criteria") @NotBlank String criteria,
    @JsonbProperty("rationale") @NotBlank String rationale
) {}
