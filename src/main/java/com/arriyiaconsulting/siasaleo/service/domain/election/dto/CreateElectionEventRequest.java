package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateElectionEventRequest(@NotNull @Positive Long electionCycleId, @NotNull LocalDate electionDate,
        @NotNull @Positive Long typeId, @Positive Long geographySnapshotId,
        @Positive Long statusId, @Size(max=2048) String sourceReference) {
    public CreateElectionEventRequest(Long cycle, LocalDate date, Long type) { this(cycle,date,type,null,null,null); }
}
