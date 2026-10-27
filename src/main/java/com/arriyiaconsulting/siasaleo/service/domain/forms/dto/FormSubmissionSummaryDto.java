package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.OffsetDateTime;

public record FormSubmissionSummaryDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("revision") Integer revision,
    @JsonbProperty("submitted_by_account_id") Long submittedByAccountId,
    @JsonbProperty("submitted_at") OffsetDateTime submittedAt
) {}
