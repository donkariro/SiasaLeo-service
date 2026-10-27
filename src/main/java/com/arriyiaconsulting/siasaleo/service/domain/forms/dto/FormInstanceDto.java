package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.JsonObject;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.OffsetDateTime;

/** What a renderer needs: the pinned definition, the draft answers and the row_version to save against. */
public record FormInstanceDto(
    @JsonbProperty("instance_id") Long instanceId,
    @JsonbProperty("form_version_id") Long formVersionId,
    @JsonbProperty("definition_checksum") String definitionChecksum,
    @JsonbProperty("definition") JsonObject definition,
    @JsonbProperty("answers") JsonObject answers,
    @JsonbProperty("row_version") Long rowVersion,
    @JsonbProperty("updated_at") OffsetDateTime updatedAt
) {}
