package com.arriyiaconsulting.siasaleo.service.electoralgeography.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Node in the electoral-area hierarchy. ancestor_path is the materialized path
 * of ancestor ids ('/' for the root, parent's path + parent id + '/' below);
 * it is computed by ElectoralAreaService, never by callers.
 */
@Entity
@Table(name = "electoral_area")
public class ElectoralArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "area_code")
    private String areaCode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "area_type_id")
    private AreaType areaType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ElectoralArea parent;

    // Read-only view of the FK so callers can get the parent id without
    // touching the lazy association.
    @Column(name = "parent_id", insertable = false, updatable = false)
    private Long parentId;

    @Column(name = "ancestor_path", length = 500)
    private String ancestorPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected ElectoralArea() {
    }

    public ElectoralArea(String name, String areaCode, AreaType areaType,
                         ElectoralArea parent, String ancestorPath) {
        this.name = name;
        this.areaCode = areaCode;
        this.areaType = areaType;
        this.parent = parent;
        this.ancestorPath = ancestorPath;
    }

    @PrePersist
    void onPersist() {
        createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public Long getParentId() {
        if (parentId != null) {
            return parentId;
        }
        return parent != null ? parent.getId() : null;
    }

    public String getAncestorPath() {
        return ancestorPath;
    }
}
