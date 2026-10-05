package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for stored_file; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "stored_file")
public class StoredFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "asset_version_id", nullable = false)
    private Long assetVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "representation_type", nullable = false, length = 30)
    private RepresentationType representationType;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "media_type", nullable = false, length = 150)
    private String mediaType;

    @Column(name = "byte_size", nullable = false)
    private Long byteSize;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt = OffsetDateTime.now(ZoneOffset.UTC);

    public StoredFile() {}

    public Long getId() { return id; }

    public Long getAssetVersionId() { return assetVersionId; }
    public void setAssetVersionId(Long value) { assetVersionId = value; }

    public RepresentationType getRepresentationType() { return representationType; }
    public void setRepresentationType(RepresentationType value) { representationType = value; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String value) { storageKey = value; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String value) { originalFilename = value; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String value) { mediaType = value; }

    public Long getByteSize() { return byteSize; }
    public void setByteSize(Long value) { byteSize = value; }

    public String getSha256() { return sha256; }
    public void setSha256(String value) { sha256 = value; }

    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(OffsetDateTime value) { uploadedAt = value; }
}
