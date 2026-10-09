package com.arriyiaconsulting.siasaleo.service.domain.party.dto;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;
public record MediaSelectionRequest(@JsonbProperty("asset_id") @NotNull @Positive Long assetId) {}
