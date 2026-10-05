package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for policy_area; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "policy_area")
public class PolicyArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    public PolicyArea() {}

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String value) { code = value; }

    public String getName() { return name; }
    public void setName(String value) { name = value; }

    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { active = value; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer value) { displayOrder = value; }
}
