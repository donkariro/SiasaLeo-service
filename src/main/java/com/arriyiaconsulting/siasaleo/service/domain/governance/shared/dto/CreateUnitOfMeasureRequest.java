package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateUnitOfMeasureRequest(
    @JsonbProperty("code") @NotBlank @Size(max = 100) String code,
    @JsonbProperty("name") @NotBlank @Size(max = 100) String name
) {}
