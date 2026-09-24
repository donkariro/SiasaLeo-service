package com.arriyiaconsulting.siasaleo.service.domain.election.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "election_event")
public class ElectionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "election_cycle_id", nullable = false)
    private ElectionCycle electionCycle;

    @Column(name = "election_date", nullable = false)
    private LocalDate electionDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type", nullable = false)
    private ElectionType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "status", nullable = false)
    private ElectionStatus status;

    @Version
    private long version;

    @Column(name = "geography_snapshot_id")
    private Long geographySnapshotId;

    @Column(name = "source_reference", length = 2048)
    private String sourceReference;

    public Long getGeographySnapshotId() { return geographySnapshotId; }
    public String getSourceReference() { return sourceReference; }

    public void assignGeography(Long snapshotId) {
        if (geographySnapshotId != null && !geographySnapshotId.equals(snapshotId))
            throw new IllegalArgumentException("Election geography is already fixed");
        geographySnapshotId = snapshotId;
    }
    public void recordSource(String source) { sourceReference = source; }

    protected ElectionEvent() { }

    public ElectionEvent(ElectionCycle electionCycle, LocalDate electionDate,
                         ElectionType type, ElectionStatus status) {
        this.electionCycle = electionCycle;
        this.electionDate = electionDate;
        this.type = type;
        this.status = status;
    }

    public Long getId() { return id; }
    public ElectionCycle getElectionCycle() { return electionCycle; }
    public LocalDate getElectionDate() { return electionDate; }
    public ElectionType getType() { return type; }
    public ElectionStatus getStatus() { return status; }

    /**
     * Moves the event along its lifecycle. Re-applying the current status is
     * a no-op rather than an error, so a retried request succeeds.
     *
     * @return whether the status changed
     */
    public boolean changeStatus(ElectionStatus next) {
        ElectionStatusName current = ElectionStatusName.of(status.getStatusName());
        ElectionStatusName target = ElectionStatusName.of(next.getStatusName());
        if (current == target) return false;
        if (!current.canChangeTo(target)) {
            throw new IllegalArgumentException("Cannot change election status from " + current + " to " + target);
        }
        status = next;
        return true;
    }
}
