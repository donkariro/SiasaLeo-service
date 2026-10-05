package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import java.util.List;

public record CommitmentDetailDto(CommitmentRevisionDto revision, List<CommitmentTargetDto> targets) {}
