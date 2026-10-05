package com.arriyiaconsulting.siasaleo.service.domain.office.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record OfficeholderEvidenceDto(
    @JsonbProperty("officeholder_id") Long officeholderId,
    @JsonbProperty("asset_version_id") Long assetVersionId,
    @JsonbProperty("locator") String locator,
    @JsonbProperty("recorded_by_account_id") Long recordedByAccountId,
    @JsonbProperty("recorded_at") OffsetDateTime recordedAt
) {}
