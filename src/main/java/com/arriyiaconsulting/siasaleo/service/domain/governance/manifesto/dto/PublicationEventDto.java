package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record PublicationEventDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("manifesto_version_id") Long manifestoVersionId,
    @JsonbProperty("announced_at") OffsetDateTime announcedAt,
    @JsonbProperty("venue") String venue,
    @JsonbProperty("asset_version_id") Long assetVersionId,
    @JsonbProperty("recorded_by_account_id") Long recordedByAccountId,
    @JsonbProperty("recorded_at") OffsetDateTime recordedAt
) {}
