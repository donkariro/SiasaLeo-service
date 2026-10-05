package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import jakarta.json.bind.annotation.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.PatchFields;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** Tracks supplied properties so omitted values and explicit null remain distinct. */
public class PatchObservationRequest extends PatchFields {
    private Long indicatorId;
    @JsonbProperty("indicator_id") public Long getIndicatorId() { return indicatorId; }
    @JsonbProperty("indicator_id") public void setIndicatorId(Long value) { mark("indicatorId"); indicatorId = value; }
    private BigDecimal actualValue;
    @JsonbProperty("actual_value") public BigDecimal getActualValue() { return actualValue; }
    @JsonbProperty("actual_value") public void setActualValue(BigDecimal value) { mark("actualValue"); actualValue = value; }
    private String qualitativeResult;
    @JsonbProperty("qualitative_result") public String getQualitativeResult() { return qualitativeResult; }
    @JsonbProperty("qualitative_result") public void setQualitativeResult(String value) { mark("qualitativeResult"); qualitativeResult = value; }
    private Long officeholderId;
    @JsonbProperty("officeholder_id") public Long getOfficeholderId() { return officeholderId; }
    @JsonbProperty("officeholder_id") public void setOfficeholderId(Long value) { mark("officeholderId"); officeholderId = value; }
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
