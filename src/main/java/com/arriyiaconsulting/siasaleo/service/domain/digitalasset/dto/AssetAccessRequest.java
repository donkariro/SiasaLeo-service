package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.AssetVisibility;
import jakarta.validation.constraints.NotNull;
public record AssetAccessRequest(@NotNull AssetVisibility visibility) {}
