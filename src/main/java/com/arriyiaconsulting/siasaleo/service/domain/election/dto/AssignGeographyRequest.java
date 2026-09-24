package com.arriyiaconsulting.siasaleo.service.domain.election.dto;
import jakarta.validation.constraints.*;
public record AssignGeographyRequest(@NotNull @Positive Long geographySnapshotId) { }
