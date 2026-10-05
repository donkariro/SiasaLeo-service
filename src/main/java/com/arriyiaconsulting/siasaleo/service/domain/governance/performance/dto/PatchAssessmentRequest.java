package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.PatchFields;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** Tracks supplied properties so omitted values and explicit null remain distinct. */
public class PatchAssessmentRequest extends PatchFields {
    private Long recordId;
    @JsonbProperty("record_id") public Long getRecordId() { return recordId; }
    @JsonbProperty("record_id") public void setRecordId(Long value) { mark("recordId"); recordId = value; }
    private Long commitmentRevisionId;
    @JsonbProperty("commitment_revision_id") public Long getCommitmentRevisionId() { return commitmentRevisionId; }
    @JsonbProperty("commitment_revision_id") public void setCommitmentRevisionId(Long value) { mark("commitmentRevisionId"); commitmentRevisionId = value; }
    private Long commitmentTargetId;
    @JsonbProperty("commitment_target_id") public Long getCommitmentTargetId() { return commitmentTargetId; }
    @JsonbProperty("commitment_target_id") public void setCommitmentTargetId(Long value) { mark("commitmentTargetId"); commitmentTargetId = value; }
    private Long observationId;
    @JsonbProperty("observation_id") public Long getObservationId() { return observationId; }
    @JsonbProperty("observation_id") public void setObservationId(Long value) { mark("observationId"); observationId = value; }
    private DeliveryStatus deliveryStatus;
    @JsonbProperty("delivery_status") public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    @JsonbProperty("delivery_status") public void setDeliveryStatus(DeliveryStatus value) { mark("deliveryStatus"); deliveryStatus = value; }
    private EvidenceStatus evidenceStatus;
    @JsonbProperty("evidence_status") public EvidenceStatus getEvidenceStatus() { return evidenceStatus; }
    @JsonbProperty("evidence_status") public void setEvidenceStatus(EvidenceStatus value) { mark("evidenceStatus"); evidenceStatus = value; }
    private String criteria;
    @JsonbProperty("criteria") public String getCriteria() { return criteria; }
    @JsonbProperty("criteria") public void setCriteria(String value) { mark("criteria"); criteria = value; }
    private String rationale;
    @JsonbProperty("rationale") public String getRationale() { return rationale; }
    @JsonbProperty("rationale") public void setRationale(String value) { mark("rationale"); rationale = value; }
}
