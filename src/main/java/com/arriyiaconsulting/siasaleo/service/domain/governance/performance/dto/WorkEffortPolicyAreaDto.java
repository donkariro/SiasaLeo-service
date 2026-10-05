package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record WorkEffortPolicyAreaDto(
    @JsonbProperty("work_effort_id") Long workEffortId,
    @JsonbProperty("policy_area_id") Long policyAreaId
) {}
