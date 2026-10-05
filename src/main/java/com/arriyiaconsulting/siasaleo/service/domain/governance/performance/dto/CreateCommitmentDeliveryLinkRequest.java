package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateCommitmentDeliveryLinkRequest(
    @JsonbProperty("commitment_revision_id") @NotNull @Positive Long commitmentRevisionId,
    @JsonbProperty("work_effort_id") @NotNull @Positive Long workEffortId,
    @JsonbProperty("relationship_type") @NotNull DeliveryRelationship relationshipType,
    @JsonbProperty("rationale") @NotBlank String rationale
) {}
