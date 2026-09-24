package com.arriyiaconsulting.siasaleo.service.domain.candidate.dto;
import jakarta.validation.constraints.*;
public record RecordBallotRequest(@NotBlank @Size(max=255) String ballotName,
        @Size(max=255) String ballotPartyName, @NotBlank @Size(max=2048) String sourceReference,
        @NotBlank @Size(max=1024) String sourceRecordReference) { }
