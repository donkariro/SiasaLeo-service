package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Optional identity match; reviewed independently of snapshot publication. */
@Entity
@Table(name = "electoral_area_correspondence")
public class ElectoralAreaCorrespondence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "electoral_area_id")
    private Long electoralAreaId;
    @Column(name = "area_snapshot_id", updatable = false)
    private Long areaSnapshotId;
    @Column(name = "evidence_reference", length = 2048)
    private String evidenceReference;
    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;
    protected ElectoralAreaCorrespondence() {}
    public ElectoralAreaCorrespondence(Long electoralAreaId, Long areaSnapshotId,
            String evidenceReference, OffsetDateTime reviewedAt) {
        this.electoralAreaId = electoralAreaId;
        this.areaSnapshotId = areaSnapshotId;
        this.evidenceReference = evidenceReference;
        this.reviewedAt = reviewedAt;
    }
    public void review(Long electoralAreaId, String evidenceReference) {
        this.electoralAreaId = electoralAreaId;
        this.evidenceReference = evidenceReference;
        this.reviewedAt = OffsetDateTime.now();
    }
    public Long getId() { return id; }
    public Long getElectoralAreaId() { return electoralAreaId; }
    public Long getAreaSnapshotId() { return areaSnapshotId; }
    public String getEvidenceReference() { return evidenceReference; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
}
