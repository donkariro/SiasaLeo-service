package com.arriyiaconsulting.siasaleo.service.domain.office.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for officeholder; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "officeholder")
public class Officeholder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "candidacy_id", nullable = true)
    private Long candidacyId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = true)
    private LocalDate endDate;

    @Column(name = "term_id", nullable = false)
    private Long termId;

    @Column(name = "entry_reason_id", nullable = false)
    private Long entryReasonId;

    @Column(name = "exit_reason_id", nullable = true)
    private Long exitReasonId;

    public Officeholder() {}

    public Long getId() { return id; }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long value) { seatId = value; }

    public Long getPersonId() { return personId; }
    public void setPersonId(Long value) { personId = value; }

    public Long getCandidacyId() { return candidacyId; }
    public void setCandidacyId(Long value) { candidacyId = value; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate value) { startDate = value; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { endDate = value; }

    public Long getTermId() { return termId; }
    public void setTermId(Long value) { termId = value; }

    public Long getEntryReasonId() { return entryReasonId; }
    public void setEntryReasonId(Long value) { entryReasonId = value; }

    public Long getExitReasonId() { return exitReasonId; }
    public void setExitReasonId(Long value) { exitReasonId = value; }
}
