package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.*;

public record CommitmentLinkRequest(@JsonbProperty("commitment_revision_id") @NotNull @Positive Long commitmentRevisionId) {}
