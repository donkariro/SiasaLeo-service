package com.arriyiaconsulting.siasaleo.service.domain.nomination.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.OffsetDateTime;

public record NominationApplicationSummaryDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("exercise_id") Long exerciseId,
    @JsonbProperty("person_id") Long personId,
    @JsonbProperty("contest_id") Long contestId,
    @JsonbProperty("status") String status,
    @JsonbProperty("created_at") OffsetDateTime createdAt,
    @JsonbProperty("submitted_at") OffsetDateTime submittedAt,
    @JsonbProperty("withdrawn_at") OffsetDateTime withdrawnAt,
    @JsonbProperty("row_version") Long rowVersion
) {}
