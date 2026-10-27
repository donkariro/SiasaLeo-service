package com.arriyiaconsulting.siasaleo.service.domain.forms.dto;

import jakarta.json.JsonObject;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.OffsetDateTime;

/** resolved is the compiled definition a renderer displays; drafts carry it too, for preview. */
public record FormVersionDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("definition_id") Long definitionId,
    @JsonbProperty("version_number") Integer versionNumber,
    @JsonbProperty("base_version_id") Long baseVersionId,
    @JsonbProperty("status") String status,
    @JsonbProperty("checksum") String checksum,
    @JsonbProperty("row_version") Long rowVersion,
    @JsonbProperty("created_at") OffsetDateTime createdAt,
    @JsonbProperty("published_at") OffsetDateTime publishedAt,
    @JsonbProperty("source") JsonObject source,
    @JsonbProperty("resolved") JsonObject resolved
) {}
