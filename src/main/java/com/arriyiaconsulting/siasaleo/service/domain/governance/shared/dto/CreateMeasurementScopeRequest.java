package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateMeasurementScopeRequest(
    @JsonbProperty("kind") @NotNull ScopeKind kind,
    @JsonbProperty("snapshot_id") @Positive Long snapshotId,
    @JsonbProperty("area_id") @Positive Long areaId,
    @JsonbProperty("population") @NotBlank String population
) {}
