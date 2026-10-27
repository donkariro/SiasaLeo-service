package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;

/** Omit both tenant fields for a platform core; give both for a tenant's extension. */
public record CreateFormDefinitionRequest(
    @JsonbProperty("code") @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z][a-z0-9-]*") String code,
    @JsonbProperty("purpose_key") @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z][a-z0-9_.-]*") String purposeKey,
    @JsonbProperty("title") @NotBlank @Size(max = 255) String title,
    @JsonbProperty("tenant_type") @Size(max = 40) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String tenantType,
    @JsonbProperty("tenant_ref") @Size(max = 100) String tenantRef
) {}
