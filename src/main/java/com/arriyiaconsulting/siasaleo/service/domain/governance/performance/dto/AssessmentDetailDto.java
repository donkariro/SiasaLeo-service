package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import java.util.List;

public record AssessmentDetailDto(AssessmentDto assessment, List<AssessmentEvidenceDto> evidence) {}
