package com.arriyiaconsulting.siasaleo.service.domain.election.dto;
import jakarta.validation.constraints.*;
public record AssignJurisdictionRequest(@NotNull @Positive Long officeId, @NotNull @Positive Long jurisdictionId) { }
