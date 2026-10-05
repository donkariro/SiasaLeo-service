package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreatePublicationEventRequest(
    @JsonbProperty("announced_at") @NotNull OffsetDateTime announcedAt,
    @JsonbProperty("venue") @NotBlank String venue,
    @JsonbProperty("asset_version_id") @Positive Long assetVersionId
) {}
