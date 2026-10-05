package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.util.List;

public record CommitmentDeliveryDto(
    @JsonbProperty("commitment_revision_id") Long commitmentRevisionId,
    @JsonbProperty("delivery_links") List<DeliveryLinkDetailDto> deliveryLinks,
    List<AssessmentDetailDto> assessments) {}
