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
    public void setStatus(ElectionStatus status) { this.status = status; }
}
