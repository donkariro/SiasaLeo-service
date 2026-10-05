package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;

public record EvidenceRequest(
    @JsonbProperty("asset_version_id") @NotNull @Positive Long assetVersionId,
    @JsonbProperty("relationship_code") @NotNull @Pattern(regexp = "SUPPORTS|CHALLENGES|CONTEXT") String relationshipCode,
    @Size(max = 255) String locator, String caption) {}
