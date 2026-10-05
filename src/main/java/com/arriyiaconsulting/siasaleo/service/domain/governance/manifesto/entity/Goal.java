package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for governance_goal; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "governance_goal")
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "manifesto_id", nullable = false)
    private Long manifestoId;

    @Column(name = "description", nullable = false)
    private String description;

    public Goal() {}

    public Long getId() { return id; }

    public Long getManifestoId() { return manifestoId; }
    public void setManifestoId(Long value) { manifestoId = value; }

    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
}
