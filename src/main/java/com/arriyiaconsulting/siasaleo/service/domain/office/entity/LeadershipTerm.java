package com.arriyiaconsulting.siasaleo.service.domain.office.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for leadership_term; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "leadership_term")
public class LeadershipTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "cycle_id", nullable = true)
    private Long cycleId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = true)
    private LocalDate endDate;

    public LeadershipTerm() {}

    public Long getId() { return id; }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long value) { seatId = value; }

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long value) { cycleId = value; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate value) { startDate = value; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { endDate = value; }
}
