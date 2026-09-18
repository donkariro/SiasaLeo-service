package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity;

import jakarta.persistence.*;

/** Historical node; identity, type and ancestry are fixed on creation. */
@Entity
@Table(name = "electoral_area_snapshot")
public class ElectoralAreaSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "snapshot_id", updatable = false)
    private Long snapshotId;
    @ManyToOne(optional = false)
    @JoinColumn(name = "area_type_id", updatable = false)
    private AreaType areaType;
    private String name;
    @Column(name = "area_code")
    private String areaCode;
    @Column(name = "parent_id", updatable = false)
    private Long parentId;
    @Column(name = "ancestor_path", updatable = false, length = 500)
    private String ancestorPath;
    @Column(name = "source_record_reference", length = 1024)
    private String sourceRecordReference;

    protected ElectoralAreaSnapshot() {}

    public ElectoralAreaSnapshot(Long snapshotId, AreaType areaType, String name, String areaCode,
            Long parentId, String ancestorPath, String sourceRecordReference) {
        this.snapshotId = snapshotId;
        this.areaType = areaType;
        this.parentId = parentId;
        this.ancestorPath = ancestorPath;
        revise(name, areaCode, sourceRecordReference);
    }

    // The service holds the owning snapshot's write lock and checks DRAFT first.
    public void revise(String name, String areaCode, String sourceRecordReference) {
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("Area name is required and must not exceed 255 characters");
        }
        if (areaCode != null && (areaCode.isBlank() || areaCode.length() > 255)) {
            throw new IllegalArgumentException("Area code must be nonblank and at most 255 characters");
        }
        if (sourceRecordReference != null && sourceRecordReference.length() > 1024) {
            throw new IllegalArgumentException("Source record reference must not exceed 1024 characters");
        }
        this.name = name.trim();
        this.areaCode = areaCode;
        this.sourceRecordReference = sourceRecordReference;
    }
    public Long getId() { return id; }
    public Long getSnapshotId() { return snapshotId; }
    public AreaType getAreaType() { return areaType; }
    public String getName() { return name; }
    public String getAreaCode() { return areaCode; }
    public Long getParentId() { return parentId; }
    public String getAncestorPath() { return ancestorPath; }
    public String getSourceRecordReference() { return sourceRecordReference; }
}
