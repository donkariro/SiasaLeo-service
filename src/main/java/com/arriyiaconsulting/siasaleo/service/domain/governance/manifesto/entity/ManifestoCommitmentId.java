package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity;

import java.io.Serializable;
import java.util.Objects;

public class ManifestoCommitmentId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long manifestoVersionId;
    public Long commitmentRevisionId;
    public ManifestoCommitmentId() {}
    public ManifestoCommitmentId(Long manifestoVersionId, Long commitmentRevisionId) {
        this.manifestoVersionId = manifestoVersionId;
        this.commitmentRevisionId = commitmentRevisionId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof ManifestoCommitmentId that && Objects.equals(manifestoVersionId, that.manifestoVersionId) && Objects.equals(commitmentRevisionId, that.commitmentRevisionId);
    }
    @Override public int hashCode() { return Objects.hash(manifestoVersionId, commitmentRevisionId); }
}
