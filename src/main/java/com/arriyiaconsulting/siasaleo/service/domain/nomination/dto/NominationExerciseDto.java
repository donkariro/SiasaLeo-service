package com.arriyiaconsulting.siasaleo.service.domain.nomination.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.OffsetDateTime;
import java.util.Map;

/** forms maps each purpose to the published form version the exercise uses. */
public record NominationExerciseDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("political_party_id") Long politicalPartyId,
    @JsonbProperty("election_event_id") Long electionEventId,
    @JsonbProperty("title") String title,
    @JsonbProperty("status") String status,
    @JsonbProperty("opens_at") OffsetDateTime opensAt,
    @JsonbProperty("closes_at") OffsetDateTime closesAt,
    @JsonbProperty("row_version") Long rowVersion,
    @JsonbProperty("forms") Map<String, Long> forms
) {}
