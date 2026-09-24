package com.arriyiaconsulting.siasaleo.service.domain.election.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The race for a specific seat in a specific election event (V8).
 */
@Entity
@Table(name = "contest")
public class Contest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "election_event_id", nullable = false, updatable = false)
    private Long electionEventId;

    @Column(name = "seat_id", updatable = false)
    private Long seatId;

    @Column(name = "office_id")
    private Long officeId;
    @Column(name = "geography_snapshot_id")
    private Long geographySnapshotId;
    @Column(name = "jurisdiction_id")
    private Long jurisdictionId;

    public Long getOfficeId() { return officeId; }
    public Long getGeographySnapshotId() { return geographySnapshotId; }
    public Long getJurisdictionId() { return jurisdictionId; }

    public void assignJurisdiction(Long office, Long snapshot, Long area) {
        if (officeId != null && !officeId.equals(office)) throw new IllegalArgumentException("Contest office must be preserved");
        if (jurisdictionId != null && (!jurisdictionId.equals(area) || !officeId.equals(office) || !geographySnapshotId.equals(snapshot)))
            throw new IllegalArgumentException("Contest jurisdiction is already fixed");
        officeId=office;
        geographySnapshotId=snapshot;
        jurisdictionId=area;
    }

    @Column(length = 255)
    private String description;

    protected Contest() {
    }

    public Contest(Long electionEventId, Long seatId, String description) {
        this.electionEventId = electionEventId;
        this.seatId = seatId;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getElectionEventId() {
        return electionEventId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public String getDescription() {
        return description;
    }
}
