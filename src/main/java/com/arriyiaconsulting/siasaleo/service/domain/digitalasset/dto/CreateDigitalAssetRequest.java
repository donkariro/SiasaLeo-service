package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import jakarta.validation.constraints.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.AssetVisibility;
import jakarta.json.bind.annotation.JsonbProperty;

public record CreateDigitalAssetRequest(
    @JsonbProperty("title") @NotBlank @Size(max = 255) String title,
    @JsonbProperty("description") String description,
    @JsonbProperty("asset_type") @NotBlank @Size(max = 40) String assetType,
    @JsonbProperty("visibility") AssetVisibility visibility,
    @JsonbProperty("owner_party_id") @Positive Long ownerPartyId
) {
    public CreateDigitalAssetRequest(String title, String description, String assetType, AssetVisibility visibility) {
        this(title, description, assetType, visibility, null);
    }
}
