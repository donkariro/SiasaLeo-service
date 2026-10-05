package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateTargetObservationRequest(
    @JsonbProperty("target_id") @NotNull @Positive Long targetId,
    @JsonbProperty("observation_id") @NotNull @Positive Long observationId
) {}
