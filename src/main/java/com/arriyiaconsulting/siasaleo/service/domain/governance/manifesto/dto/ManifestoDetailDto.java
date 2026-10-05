package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import java.util.List;

public record ManifestoDetailDto(ManifestoVersionDto version, List<CommitmentDetailDto> commitments,
                                 List<ManifestoAttachmentDto> assets) {}
