package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record ManifestoVersionDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("manifesto_id") Long manifestoId,
    @JsonbProperty("version_number") Integer versionNumber,
    @JsonbProperty("title") String title,
    @JsonbProperty("language") String language,
    @JsonbProperty("content") String content,
    @JsonbProperty("publication_status") PublicationStatus publicationStatus,
    @JsonbProperty("published_at") OffsetDateTime publishedAt,
    @JsonbProperty("recorded_by_account_id") Long recordedByAccountId,
    @JsonbProperty("created_at") OffsetDateTime createdAt,
    @JsonbProperty("row_version") Long rowVersion
) {}
