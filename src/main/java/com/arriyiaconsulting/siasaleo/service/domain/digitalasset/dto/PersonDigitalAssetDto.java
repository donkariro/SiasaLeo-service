package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import jakarta.json.bind.annotation.JsonbProperty;

public record PersonDigitalAssetDto(
    @JsonbProperty("person_id") Long personId,
    @JsonbProperty("asset_version_id") Long assetVersionId,
    @JsonbProperty("purpose") String purpose,
    @JsonbProperty("caption") String caption
) {}
