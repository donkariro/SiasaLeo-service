package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.RepresentationType;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;

public record StoredFileDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("asset_version_id") Long assetVersionId,
    @JsonbProperty("representation_type") RepresentationType representationType,
    @JsonbProperty("original_filename") String originalFilename,
    @JsonbProperty("media_type") String mediaType,
    @JsonbProperty("byte_size") Long byteSize,
    @JsonbProperty("sha256") String sha256,
    @JsonbProperty("uploaded_at") OffsetDateTime uploadedAt
) {}
