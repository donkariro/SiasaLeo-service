package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;

public record GoalLinkRequest(@JsonbProperty("goal_id") @NotNull @Positive Long goalId) {}
