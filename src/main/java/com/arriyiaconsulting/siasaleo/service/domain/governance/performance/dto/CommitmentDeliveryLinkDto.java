package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CommitmentDeliveryLinkDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("commitment_revision_id") Long commitmentRevisionId,
    @JsonbProperty("work_effort_id") Long workEffortId,
    @JsonbProperty("relationship_type") DeliveryRelationship relationshipType,
    @JsonbProperty("rationale") String rationale,
    @JsonbProperty("recorded_by_account_id") Long recordedByAccountId,
    @JsonbProperty("created_at") OffsetDateTime createdAt
) {}
