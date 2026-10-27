package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.OffsetDateTime;

public record FormDefinitionDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("code") String code,
    @JsonbProperty("purpose_key") String purposeKey,
    @JsonbProperty("tenant_id") Long tenantId,
    @JsonbProperty("title") String title,
    @JsonbProperty("created_at") OffsetDateTime createdAt
) {}
