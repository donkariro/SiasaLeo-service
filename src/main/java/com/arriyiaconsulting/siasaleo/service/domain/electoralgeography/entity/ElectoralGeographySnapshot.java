package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "electoral_geography_snapshot")
public class ElectoralGeographySnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(name = "reference_date")
    private LocalDate referenceDate;
    @Column(name = "source_reference", length = 2048)
    private String sourceReference;
    @Column(name = "source_sha256", length = 64)
    private String sourceSha256;
    @Column(name = "supersedes_snapshot_id", updatable = false)
    private Long supersedesSnapshotId;
    @Enumerated(EnumType.STRING)
    private SnapshotStatus status = SnapshotStatus.DRAFT;
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
    @Version
    private long version;

    protected ElectoralGeographySnapshot() {}

    public ElectoralGeographySnapshot(String name, LocalDate referenceDate, String sourceReference,
            String sourceSha256, Long supersedesSnapshotId) {
        revise(name, referenceDate, sourceReference, sourceSha256);
        this.supersedesSnapshotId = supersedesSnapshotId;
    }

    public void requireDraft() {
        if (status != SnapshotStatus.DRAFT) {
            throw new IllegalArgumentException("Published snapshots are immutable; create a revision");
        }
    }

    public void revise(String name, LocalDate referenceDate, String sourceReference, String sourceSha256) {
        requireDraft();
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("Snapshot name is required and must not exceed 255 characters");
        }
        if (sourceReference != null && (sourceReference.isBlank() || sourceReference.length() > 2048)) {
            throw new IllegalArgumentException("Source reference must be nonblank and at most 2048 characters");
        }
        if (sourceSha256 != null && !sourceSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Source checksum must be a lowercase SHA-256 digest");
        }
        this.name = name.trim();
        this.referenceDate = referenceDate;
        this.sourceReference = sourceReference;
        this.sourceSha256 = sourceSha256;
    }

    public void publish() {
        requireDraft();
        if (sourceReference == null || sourceSha256 == null) {
            throw new IllegalArgumentException("Publication requires a source reference and SHA-256 checksum");
        }
        status = SnapshotStatus.PUBLISHED;
        publishedAt = OffsetDateTime.now();
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDate getReferenceDate() { return referenceDate; }
    public String getSourceReference() { return sourceReference; }
    public String getSourceSha256() { return sourceSha256; }
    public Long getSupersedesSnapshotId() { return supersedesSnapshotId; }
    public SnapshotStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
}
