package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;
import jakarta.validation.constraints.NotNull;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.AssetPermission;
public record AssetGrantRequest(@NotNull AssetPermission permission) {}
