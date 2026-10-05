package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record ManifestoCommitmentDto(
    @JsonbProperty("manifesto_version_id") Long manifestoVersionId,
    @JsonbProperty("commitment_revision_id") Long commitmentRevisionId
) {}
