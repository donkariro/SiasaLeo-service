package com.arriyiaconsulting.siasaleo.service.domain.nomination.dto;

import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.FormInstanceDto;
import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.FormSubmissionSummaryDto;
import jakarta.json.bind.annotation.JsonbProperty;
import java.util.List;

/** The application with its form: definition, draft answers and submitted revisions. */
public record NominationApplicationDto(
    @JsonbProperty("application") NominationApplicationSummaryDto application,
    @JsonbProperty("form") FormInstanceDto form,
    @JsonbProperty("submissions") List<FormSubmissionSummaryDto> submissions
) {}
