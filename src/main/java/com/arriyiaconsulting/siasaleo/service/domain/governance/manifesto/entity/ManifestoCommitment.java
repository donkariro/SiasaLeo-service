package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for manifesto_commitment; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "manifesto_commitment")
@IdClass(ManifestoCommitmentId.class)
public class ManifestoCommitment {
    @Id
    @Column(name = "manifesto_version_id", nullable = false)
    private Long manifestoVersionId;

    @Id
    @Column(name = "commitment_revision_id", nullable = false)
    private Long commitmentRevisionId;

    public ManifestoCommitment() {}

    public Long getManifestoVersionId() { return manifestoVersionId; }
    public void setManifestoVersionId(Long value) { manifestoVersionId = value; }

    public Long getCommitmentRevisionId() { return commitmentRevisionId; }
    public void setCommitmentRevisionId(Long value) { commitmentRevisionId = value; }
}
