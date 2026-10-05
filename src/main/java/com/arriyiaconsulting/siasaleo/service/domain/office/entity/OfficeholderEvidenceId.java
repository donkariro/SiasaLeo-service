package com.arriyiaconsulting.siasaleo.service.domain.office.entity;

import java.io.Serializable;
import java.util.Objects;

public class OfficeholderEvidenceId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long officeholderId;
    public Long assetVersionId;
    public OfficeholderEvidenceId() {}
    public OfficeholderEvidenceId(Long officeholderId, Long assetVersionId) {
        this.officeholderId = officeholderId;
        this.assetVersionId = assetVersionId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof OfficeholderEvidenceId that && Objects.equals(officeholderId, that.officeholderId) && Objects.equals(assetVersionId, that.assetVersionId);
    }
    @Override public int hashCode() { return Objects.hash(officeholderId, assetVersionId); }
}
