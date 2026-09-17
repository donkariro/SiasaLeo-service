package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import jakarta.validation.constraints.*;

public record ChangeElectionStatusRequest(@NotNull @Positive Long statusId) { }
