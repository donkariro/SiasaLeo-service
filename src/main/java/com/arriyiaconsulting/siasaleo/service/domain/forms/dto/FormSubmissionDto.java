package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.JsonObject;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * An immutable submitted revision. documents maps document fields to the
 * sealed asset version IDs submitted; declarations holds the exact wording
 * accepted. context is the consumer's snapshot of related records.
 */
public record FormSubmissionDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("revision") Integer revision,
    @JsonbProperty("form_version_id") Long formVersionId,
    @JsonbProperty("definition_checksum") String definitionChecksum,
    @JsonbProperty("answers") JsonObject answers,
    @JsonbProperty("context") JsonObject context,
    @JsonbProperty("documents") Map<String, List<Long>> documents,
    @JsonbProperty("declarations") Map<String, String> declarations,
    @JsonbProperty("submitted_by_account_id") Long submittedByAccountId,
    @JsonbProperty("submitted_at") OffsetDateTime submittedAt
) {}
