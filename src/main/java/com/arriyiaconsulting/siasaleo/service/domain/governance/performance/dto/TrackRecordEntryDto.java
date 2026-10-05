package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import com.arriyiaconsulting.siasaleo.service.domain.office.dto.OfficeholderDto;
import java.util.List;

public record TrackRecordEntryDto(ClaimRevisionDto claim, OfficeholderDto officeholder,
                                  List<ClaimEvidenceDto> evidence, List<AssessmentDetailDto> assessments) {}
