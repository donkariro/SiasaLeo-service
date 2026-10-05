package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for measurement_scope; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "measurement_scope")
public class MeasurementScope {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private ScopeKind kind;

    @Column(name = "snapshot_id", nullable = true)
    private Long snapshotId;

    @Column(name = "area_id", nullable = true)
    private Long areaId;

    @Column(name = "population", nullable = false)
    private String population;

    public MeasurementScope() {}

    public Long getId() { return id; }

    public ScopeKind getKind() { return kind; }
    public void setKind(ScopeKind value) { kind = value; }

    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long value) { snapshotId = value; }

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long value) { areaId = value; }

    public String getPopulation() { return population; }
    public void setPopulation(String value) { population = value; }
}
