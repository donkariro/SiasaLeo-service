package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record IndicatorDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("code") String code,
    @JsonbProperty("name") String name,
    @JsonbProperty("description") String description,
    @JsonbProperty("unit") String unit,
    @JsonbProperty("methodology") String methodology,
    @JsonbProperty("direction") IndicatorDirection direction,
    @JsonbProperty("created_at") OffsetDateTime createdAt,
    @JsonbProperty("unit_id") Long unitId
) {}
