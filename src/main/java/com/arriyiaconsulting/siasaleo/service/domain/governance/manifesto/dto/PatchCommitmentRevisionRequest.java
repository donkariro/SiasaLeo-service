package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.PatchFields;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

/** Tracks supplied properties so omitted values and explicit null remain distinct. */
public class PatchCommitmentRevisionRequest extends PatchFields {
    private String wording;
    @JsonbProperty("wording") public String getWording() { return wording; }
    @JsonbProperty("wording") public void setWording(String value) { mark("wording"); wording = value; }
    private String conditions;
    @JsonbProperty("conditions") public String getConditions() { return conditions; }
    @JsonbProperty("conditions") public void setConditions(String value) { mark("conditions"); conditions = value; }
    private LocalDate periodStart;
    @JsonbProperty("period_start") public LocalDate getPeriodStart() { return periodStart; }
    @JsonbProperty("period_start") public void setPeriodStart(LocalDate value) { mark("periodStart"); periodStart = value; }
    private LocalDate periodEnd;
    @JsonbProperty("period_end") public LocalDate getPeriodEnd() { return periodEnd; }
    @JsonbProperty("period_end") public void setPeriodEnd(LocalDate value) { mark("periodEnd"); periodEnd = value; }
    private DatePrecision datePrecision;
    @JsonbProperty("date_precision") public DatePrecision getDatePrecision() { return datePrecision; }
    @JsonbProperty("date_precision") public void setDatePrecision(DatePrecision value) { mark("datePrecision"); datePrecision = value; }
    private Long scopeId;
    @JsonbProperty("scope_id") public Long getScopeId() { return scopeId; }
    @JsonbProperty("scope_id") public void setScopeId(Long value) { mark("scopeId"); scopeId = value; }
}
