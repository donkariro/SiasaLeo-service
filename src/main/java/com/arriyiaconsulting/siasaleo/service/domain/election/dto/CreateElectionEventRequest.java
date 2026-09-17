package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateElectionEventRequest(@NotNull @Positive Long electionCycleId, @NotNull LocalDate electionDate,
        @NotNull @Positive Long typeId) { }
