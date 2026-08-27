package com.arriyiaconsulting.siasaleo.service.domain.election.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The race for a specific seat in a specific election event (V8). Minimal
 * read-only mapping for now: contests are referenced by candidacies but not
 * yet managed through the API, so the election_event/seat associations stay
 * plain ids until the election domain is built out.
 */
@Entity
@Table(name = "contest")
public class Contest {

    @Id
    private Long id;

    @Column(name = "election_event_id", insertable = false, updatable = false)
    private Long electionEventId;

    @Column(name = "seat_id", insertable = false, updatable = false)
    private Long seatId;

    @Column(length = 255)
    private String description;

    protected Contest() {
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
