package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** JPA mapping for person_digital_asset; foreign-key IDs follow the existing Seat/Contest convention. */
@Entity
@Table(name = "person_digital_asset")
@IdClass(PersonDigitalAssetId.class)
public class PersonDigitalAsset {
    @Id
    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Id
    @Column(name = "asset_version_id", nullable = false)
    private Long assetVersionId;

    @Id
    @Column(name = "purpose", nullable = false, length = 100)
    private String purpose;

    @Column(name = "caption", nullable = true)
    private String caption;

    public PersonDigitalAsset() {}

    public Long getPersonId() { return personId; }
    public void setPersonId(Long value) { personId = value; }

    public Long getAssetVersionId() { return assetVersionId; }
    public void setAssetVersionId(Long value) { assetVersionId = value; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String value) { purpose = value; }

    public String getCaption() { return caption; }
    public void setCaption(String value) { caption = value; }
}
