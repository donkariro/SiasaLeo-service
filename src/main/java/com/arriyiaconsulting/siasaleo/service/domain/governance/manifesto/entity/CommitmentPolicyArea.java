package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for commitment_policy_area; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "commitment_policy_area")
@IdClass(CommitmentPolicyAreaId.class)
public class CommitmentPolicyArea {
    @Id
    @Column(name = "commitment_revision_id", nullable = false)
    private Long commitmentRevisionId;

    @Id
    @Column(name = "policy_area_id", nullable = false)
    private Long policyAreaId;

    public CommitmentPolicyArea() {}

    public Long getCommitmentRevisionId() { return commitmentRevisionId; }
    public void setCommitmentRevisionId(Long value) { commitmentRevisionId = value; }

    public Long getPolicyAreaId() { return policyAreaId; }
    public void setPolicyAreaId(Long value) { policyAreaId = value; }
}
