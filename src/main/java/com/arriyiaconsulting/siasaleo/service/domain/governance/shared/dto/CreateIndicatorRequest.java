package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateIndicatorRequest(
    @JsonbProperty("code") @NotBlank @Size(max = 100) String code,
    @JsonbProperty("name") @NotBlank @Size(max = 255) String name,
    @JsonbProperty("unit") @NotBlank @Size(max = 100) String unit,
    @JsonbProperty("unit_id") @Positive Long unitId,
    @JsonbProperty("methodology") @NotBlank String methodology,
    @JsonbProperty("direction") @NotNull IndicatorDirection direction,
    @JsonbProperty("description") String description
) {}
