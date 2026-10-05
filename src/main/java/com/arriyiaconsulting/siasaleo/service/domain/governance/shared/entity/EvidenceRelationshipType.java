package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for evidence_relationship_type; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "evidence_relationship_type")
public class EvidenceRelationshipType {
    @Id
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    public EvidenceRelationshipType() {}

    public String getCode() { return code; }
    public void setCode(String value) { code = value; }

    public String getName() { return name; }
    public void setName(String value) { name = value; }
}
