package com.arriyiaconsulting.siasaleo.service.domain.party.dto;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;
public record MediaAttachmentRequest(@JsonbProperty("asset_version_id") @NotNull @Positive Long assetVersionId, String caption) {}
