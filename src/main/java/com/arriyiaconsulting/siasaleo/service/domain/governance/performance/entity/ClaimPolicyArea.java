package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for claim_policy_area; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "claim_policy_area")
@IdClass(ClaimPolicyAreaId.class)
public class ClaimPolicyArea {
    @Id
    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Id
    @Column(name = "policy_area_id", nullable = false)
    private Long policyAreaId;

    public ClaimPolicyArea() {}

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long value) { recordId = value; }

    public Long getPolicyAreaId() { return policyAreaId; }
    public void setPolicyAreaId(Long value) { policyAreaId = value; }
}
