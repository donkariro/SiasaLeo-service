package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record ManifestoDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("issuer_person_id") Long issuerPersonId,
    @JsonbProperty("candidacy_id") Long candidacyId,
    @JsonbProperty("created_by_account_id") Long createdByAccountId,
    @JsonbProperty("created_at") OffsetDateTime createdAt
) {}
