package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;

public record PolicyAreaRequest(@JsonbProperty("policy_area_id") @NotNull @Positive Long policyAreaId) {}
