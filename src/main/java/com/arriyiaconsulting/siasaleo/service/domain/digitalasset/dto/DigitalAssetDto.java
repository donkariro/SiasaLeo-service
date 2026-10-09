package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.AssetVisibility;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;

public record DigitalAssetDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("title") String title,
    @JsonbProperty("description") String description,
    @JsonbProperty("asset_type") String assetType,
    @JsonbProperty("visibility") AssetVisibility visibility,
    @JsonbProperty("created_by_account_id") Long createdByAccountId,
    @JsonbProperty("created_at") OffsetDateTime createdAt,
    @JsonbProperty("owner_account_id") Long ownerAccountId,
    @JsonbProperty("owner_party_id") Long ownerPartyId,
    @JsonbProperty("archived_at") OffsetDateTime archivedAt,
    @JsonbProperty("row_version") Long rowVersion
) {}
