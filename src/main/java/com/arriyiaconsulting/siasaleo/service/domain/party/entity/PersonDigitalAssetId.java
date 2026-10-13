package com.arriyiaconsulting.siasaleo.service.domain.party.entity;

import java.io.Serializable;
import java.util.Objects;

public class PersonDigitalAssetId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long personId;
    public Long assetVersionId;
    public String purpose;
    public PersonDigitalAssetId() {}
    public PersonDigitalAssetId(Long personId, Long assetVersionId, String purpose) {
        this.personId = personId;
        this.assetVersionId = assetVersionId;
        this.purpose = purpose;
    }
    @Override public boolean equals(Object other) {
        return other instanceof PersonDigitalAssetId that && Objects.equals(personId, that.personId) && Objects.equals(assetVersionId, that.assetVersionId) && Objects.equals(purpose, that.purpose);
    }
    @Override public int hashCode() { return Objects.hash(personId, assetVersionId, purpose); }
}
