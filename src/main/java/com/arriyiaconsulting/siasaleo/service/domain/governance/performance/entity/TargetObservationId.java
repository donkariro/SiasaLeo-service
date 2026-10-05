package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity;

import java.io.Serializable;
import java.util.Objects;

public class TargetObservationId implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long targetId;
    public Long observationId;
    public TargetObservationId() {}
    public TargetObservationId(Long targetId, Long observationId) {
        this.targetId = targetId;
        this.observationId = observationId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof TargetObservationId that && Objects.equals(targetId, that.targetId) && Objects.equals(observationId, that.observationId);
    }
    @Override public int hashCode() { return Objects.hash(targetId, observationId); }
}
