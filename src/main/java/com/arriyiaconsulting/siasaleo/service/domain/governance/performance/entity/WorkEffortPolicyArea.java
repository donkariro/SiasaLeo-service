package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for work_effort_policy_area; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "work_effort_policy_area")
@IdClass(WorkEffortPolicyAreaId.class)
public class WorkEffortPolicyArea {
    @Id
    @Column(name = "work_effort_id", nullable = false)
    private Long workEffortId;

    @Id
    @Column(name = "policy_area_id", nullable = false)
    private Long policyAreaId;

    public WorkEffortPolicyArea() {}

    public Long getWorkEffortId() { return workEffortId; }
    public void setWorkEffortId(Long value) { workEffortId = value; }

    public Long getPolicyAreaId() { return policyAreaId; }
    public void setPolicyAreaId(Long value) { policyAreaId = value; }
}
