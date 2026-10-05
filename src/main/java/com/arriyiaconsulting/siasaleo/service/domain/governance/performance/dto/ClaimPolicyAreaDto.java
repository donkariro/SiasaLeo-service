package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record ClaimPolicyAreaDto(
    @JsonbProperty("record_id") Long recordId,
    @JsonbProperty("policy_area_id") Long policyAreaId
) {}
