package com.arriyiaconsulting.siasaleo.service.domain.nomination.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record CreateNominationExerciseRequest(
    @JsonbProperty("political_party_id") @NotNull @Positive Long politicalPartyId,
    @JsonbProperty("election_event_id") @NotNull @Positive Long electionEventId,
    @JsonbProperty("title") @NotBlank @Size(max = 255) String title,
    @JsonbProperty("opens_at") @NotNull OffsetDateTime opensAt,
    @JsonbProperty("closes_at") @NotNull OffsetDateTime closesAt
) {}
