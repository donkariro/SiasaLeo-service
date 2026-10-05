package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record MeasurementScopeDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("kind") ScopeKind kind,
    @JsonbProperty("snapshot_id") Long snapshotId,
    @JsonbProperty("area_id") Long areaId,
    @JsonbProperty("population") String population
) {}
