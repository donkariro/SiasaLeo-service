package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;
public record UpdateAssetRequest(
 @NotBlank @Size(max=255) String title, String description,
 @JsonbProperty("asset_type") @NotBlank @Size(max=40) String assetType,
 @JsonbProperty("row_version") @NotNull @PositiveOrZero Long rowVersion) {}
