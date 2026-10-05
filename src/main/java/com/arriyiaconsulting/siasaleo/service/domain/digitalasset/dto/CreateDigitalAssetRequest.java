package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateDigitalAssetRequest(
    @JsonbProperty("title") @NotBlank @Size(max = 255) String title,
    @JsonbProperty("description") String description,
    @JsonbProperty("asset_type") @NotBlank @Size(max = 40) String assetType
) {}
