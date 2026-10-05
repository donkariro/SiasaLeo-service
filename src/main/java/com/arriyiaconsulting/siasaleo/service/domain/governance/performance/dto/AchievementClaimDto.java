package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record AchievementClaimDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("officeholder_id") Long officeholderId,
    @JsonbProperty("created_at") OffsetDateTime createdAt
) {}
