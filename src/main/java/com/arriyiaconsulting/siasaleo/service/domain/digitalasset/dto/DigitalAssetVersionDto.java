package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record DigitalAssetVersionDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("asset_id") Long assetId,
    @JsonbProperty("version_number") Integer versionNumber,
    @JsonbProperty("source_url") String sourceUrl,
    @JsonbProperty("author_party_id") Long authorPartyId,
    @JsonbProperty("publisher_party_id") Long publisherPartyId,
    @JsonbProperty("source_published_on") LocalDate sourcePublishedOn,
    @JsonbProperty("provenance") String provenance,
    @JsonbProperty("recorded_by_account_id") Long recordedByAccountId,
    @JsonbProperty("recorded_at") OffsetDateTime recordedAt,
    @JsonbProperty("sealed_at") OffsetDateTime sealedAt,
    @JsonbProperty("row_version") Long rowVersion
) {}
