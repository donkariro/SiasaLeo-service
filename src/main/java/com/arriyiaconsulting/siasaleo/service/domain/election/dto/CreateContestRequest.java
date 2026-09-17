package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import jakarta.validation.constraints.*;

public record CreateContestRequest(@NotNull @Positive Long electionEventId, @NotNull @Positive Long seatId,
        @Size(max = 255) String description) { }
