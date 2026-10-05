package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record WorkEffortAssetDto(
    @JsonbProperty("work_effort_id") Long workEffortId,
    @JsonbProperty("asset_version_id") Long assetVersionId,
    @JsonbProperty("caption") String caption,
    @JsonbProperty("locator") String locator
) {}
