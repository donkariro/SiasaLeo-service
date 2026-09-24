package com.arriyiaconsulting.siasaleo.service.domain.election.dto;

import jakarta.validation.constraints.*;

public record CreateContestRequest(@NotNull @Positive Long electionEventId, @Positive Long seatId,
        @Size(max = 255) String description, @Positive Long officeId, @Positive Long jurisdictionId) {
    public CreateContestRequest(Long event, Long seat, String description) { this(event,seat,description,null,null); }
}
