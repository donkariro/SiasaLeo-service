package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.PatchFields;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** Tracks supplied properties so omitted values and explicit null remain distinct. */
public class PatchClaimRevisionRequest extends PatchFields {
    private String title;
    @JsonbProperty("title") public String getTitle() { return title; }
    @JsonbProperty("title") public void setTitle(String value) { mark("title"); title = value; }
    private String description;
    @JsonbProperty("description") public String getDescription() { return description; }
    @JsonbProperty("description") public void setDescription(String value) { mark("description"); description = value; }
    private Long claimantPartyId;
    @JsonbProperty("claimant_party_id") public Long getClaimantPartyId() { return claimantPartyId; }
    @JsonbProperty("claimant_party_id") public void setClaimantPartyId(Long value) { mark("claimantPartyId"); claimantPartyId = value; }
    private Long workEffortId;
    @JsonbProperty("work_effort_id") public Long getWorkEffortId() { return workEffortId; }
    @JsonbProperty("work_effort_id") public void setWorkEffortId(Long value) { mark("workEffortId"); workEffortId = value; }
    private Long scopeId;
    @JsonbProperty("scope_id") public Long getScopeId() { return scopeId; }
    @JsonbProperty("scope_id") public void setScopeId(Long value) { mark("scopeId"); scopeId = value; }
    private LocalDate periodStart;
    @JsonbProperty("period_start") public LocalDate getPeriodStart() { return periodStart; }
    @JsonbProperty("period_start") public void setPeriodStart(LocalDate value) { mark("periodStart"); periodStart = value; }
    private LocalDate periodEnd;
    @JsonbProperty("period_end") public LocalDate getPeriodEnd() { return periodEnd; }
    @JsonbProperty("period_end") public void setPeriodEnd(LocalDate value) { mark("periodEnd"); periodEnd = value; }
    private DatePrecision datePrecision;
    @JsonbProperty("date_precision") public DatePrecision getDatePrecision() { return datePrecision; }
    @JsonbProperty("date_precision") public void setDatePrecision(DatePrecision value) { mark("datePrecision"); datePrecision = value; }
    private String sourceReference;
    @JsonbProperty("source_reference") public String getSourceReference() { return sourceReference; }
    @JsonbProperty("source_reference") public void setSourceReference(String value) { mark("sourceReference"); sourceReference = value; }
}
