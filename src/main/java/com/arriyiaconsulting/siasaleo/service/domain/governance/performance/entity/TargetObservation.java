package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for target_observation; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "target_observation")
@IdClass(TargetObservationId.class)
public class TargetObservation {
    @Id
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Id
    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    public TargetObservation() {}

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long value) { targetId = value; }

    public Long getObservationId() { return observationId; }
    public void setObservationId(Long value) { observationId = value; }
}
