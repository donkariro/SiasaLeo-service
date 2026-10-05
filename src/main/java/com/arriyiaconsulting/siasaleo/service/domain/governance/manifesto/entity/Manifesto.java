package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for manifesto; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "manifesto")
public class Manifesto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "issuer_person_id", nullable = false)
    private Long issuerPersonId;

    @Column(name = "candidacy_id", nullable = false)
    private Long candidacyId;

    @Column(name = "created_by_account_id", nullable = false)
    private Long createdByAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    public Manifesto() {}

    public Long getId() { return id; }

    public Long getIssuerPersonId() { return issuerPersonId; }
    public void setIssuerPersonId(Long value) { issuerPersonId = value; }

    public Long getCandidacyId() { return candidacyId; }
    public void setCandidacyId(Long value) { candidacyId = value; }

    public Long getCreatedByAccountId() { return createdByAccountId; }
    public void setCreatedByAccountId(Long value) { createdByAccountId = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }
}
