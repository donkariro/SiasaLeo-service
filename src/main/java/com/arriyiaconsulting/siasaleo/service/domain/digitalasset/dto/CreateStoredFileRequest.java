package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import jakarta.validation.constraints.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.RepresentationType;
import jakarta.json.bind.annotation.JsonbProperty;

public record CreateStoredFileRequest(
    @JsonbProperty("representation_type") @NotNull RepresentationType representationType,
    @JsonbProperty("storage_key") @NotBlank @Size(max = 1024) String storageKey,
    @JsonbProperty("original_filename") @NotBlank @Size(max = 255) String originalFilename,
    @JsonbProperty("media_type") @NotBlank @Size(max = 150) String mediaType,
    @JsonbProperty("byte_size") @NotNull @Positive Long byteSize,
    @JsonbProperty("sha256") @NotBlank @Size(max = 64) @Pattern(regexp = "^[0-9a-f]{64}$") String sha256
) {}
