package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for commitment; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "commitment")
public class Commitment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "manifesto_id", nullable = false)
    private Long manifestoId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public Commitment() {}

    public Long getId() { return id; }

    public Long getManifestoId() { return manifestoId; }
    public void setManifestoId(Long value) { manifestoId = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }
}
