package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import java.io.Serializable;
import java.util.Objects;

public class ManifestoAttachmentId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long manifestoVersionId;
    public Long assetVersionId;
    public ManifestoAttachmentId() {}
    public ManifestoAttachmentId(Long manifestoVersionId, Long assetVersionId) {
        this.manifestoVersionId = manifestoVersionId;
        this.assetVersionId = assetVersionId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof ManifestoAttachmentId that && Objects.equals(manifestoVersionId, that.manifestoVersionId) && Objects.equals(assetVersionId, that.assetVersionId);
    }
    @Override public int hashCode() { return Objects.hash(manifestoVersionId, assetVersionId); }
}
