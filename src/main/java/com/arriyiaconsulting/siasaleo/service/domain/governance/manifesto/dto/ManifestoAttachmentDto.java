package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record ManifestoAttachmentDto(
    @JsonbProperty("manifesto_version_id") Long manifestoVersionId,
    @JsonbProperty("asset_version_id") Long assetVersionId,
    @JsonbProperty("caption") String caption,
    @JsonbProperty("locator") String locator
) {}
