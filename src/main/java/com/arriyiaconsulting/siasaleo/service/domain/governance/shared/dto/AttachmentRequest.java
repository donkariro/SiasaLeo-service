package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;

public record AttachmentRequest(
    @JsonbProperty("asset_version_id") @NotNull @Positive Long assetVersionId,
    @Size(max = 255) String locator, String caption) {}
