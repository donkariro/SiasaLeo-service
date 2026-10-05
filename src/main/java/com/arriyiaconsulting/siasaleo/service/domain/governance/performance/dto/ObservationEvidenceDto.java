package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record ObservationEvidenceDto(
    @JsonbProperty("observation_id") Long observationId,
    @JsonbProperty("asset_version_id") Long assetVersionId,
    @JsonbProperty("relationship_code") String relationshipCode,
    @JsonbProperty("locator") String locator,
    @JsonbProperty("caption") String caption
) {}
