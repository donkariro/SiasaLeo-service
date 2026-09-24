package com.arriyiaconsulting.siasaleo.service.domain.election.dto;
import jakarta.validation.constraints.*;
public final class RegisterAssignmentDtos {
    private RegisterAssignmentDtos() { }
    public record AssignmentRequest(@NotNull Long registerId, Long supersedesId,
            @NotBlank @Size(max=2048) String sourceReference) { }
    public record AssignmentDto(Long id, Long electionEventId, Long registerId, Long supersedesId,
            String sourceReference, String assignedAt) { }
}
