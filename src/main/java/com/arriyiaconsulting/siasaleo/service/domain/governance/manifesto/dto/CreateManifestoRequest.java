package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateManifestoRequest(
    @JsonbProperty("candidacy_id") @NotNull @Positive Long candidacyId
) {}
