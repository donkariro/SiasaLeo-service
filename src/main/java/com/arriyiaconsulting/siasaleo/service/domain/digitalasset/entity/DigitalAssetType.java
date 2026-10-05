package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for asset_type; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "digital_asset_type")
public class DigitalAssetType {
    @Id
    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public DigitalAssetType() {}

    public String getCode() { return code; }
    public void setCode(String value) { code = value; }

    public String getName() { return name; }
    public void setName(String value) { name = value; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { active = value; }
}
