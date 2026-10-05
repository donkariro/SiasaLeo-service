package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateManifestoVersionRequest(
    @JsonbProperty("title") @NotBlank @Size(max = 255) String title,
    @JsonbProperty("language") @Size(max = 35) String language,
    @JsonbProperty("content") @NotBlank String content
) {}
