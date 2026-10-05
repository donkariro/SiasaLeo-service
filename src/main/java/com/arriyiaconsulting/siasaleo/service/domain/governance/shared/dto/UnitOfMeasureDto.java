package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record UnitOfMeasureDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("code") String code,
    @JsonbProperty("name") String name,
    @JsonbProperty("active") Boolean active
) {}
