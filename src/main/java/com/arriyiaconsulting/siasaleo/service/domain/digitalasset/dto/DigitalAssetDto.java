package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record DigitalAssetDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("title") String title,
    @JsonbProperty("description") String description,
    @JsonbProperty("asset_type") String assetType,
    @JsonbProperty("created_by_account_id") Long createdByAccountId,
    @JsonbProperty("created_at") OffsetDateTime createdAt
) {}
