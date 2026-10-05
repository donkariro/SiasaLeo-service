package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import java.io.Serializable;
import java.util.Objects;

public class CommitmentAttachmentId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long commitmentRevisionId;
    public Long assetVersionId;
    public CommitmentAttachmentId() {}
    public CommitmentAttachmentId(Long commitmentRevisionId, Long assetVersionId) {
        this.commitmentRevisionId = commitmentRevisionId;
        this.assetVersionId = assetVersionId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof CommitmentAttachmentId that && Objects.equals(commitmentRevisionId, that.commitmentRevisionId) && Objects.equals(assetVersionId, that.assetVersionId);
    }
    @Override public int hashCode() { return Objects.hash(commitmentRevisionId, assetVersionId); }
}
