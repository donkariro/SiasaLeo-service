package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for performance_indicator; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "performance_indicator")
public class Indicator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "unit", nullable = false, length = 100)
    private String unit;

    @Column(name = "methodology", nullable = false)
    private String methodology;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private IndicatorDirection direction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    public Indicator() {}

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String value) { code = value; }

    public String getName() { return name; }
    public void setName(String value) { name = value; }

    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }

    public String getUnit() { return unit; }
    public void setUnit(String value) { unit = value; }

    public String getMethodology() { return methodology; }
    public void setMethodology(String value) { methodology = value; }

    public IndicatorDirection getDirection() { return direction; }
    public void setDirection(IndicatorDirection value) { direction = value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { createdAt = value; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long value) { unitId = value; }
}
