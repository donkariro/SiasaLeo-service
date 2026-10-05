package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto;

import java.util.List;

public record DeliveryLinkDetailDto(CommitmentDeliveryLinkDto link, WorkEffortDto workEffort,
                                    List<WorkEffortParticipationDto> participants) {}
