package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record TargetObservationDto(
    @JsonbProperty("target_id") Long targetId,
    @JsonbProperty("observation_id") Long observationId
) {}
