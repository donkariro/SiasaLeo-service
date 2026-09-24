package com.arriyiaconsulting.siasaleo.service.domain.result.dto;
import java.math.BigDecimal;
import java.util.List;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport.ValidationReport;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.Coverage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.validation.EnumName;
import com.arriyiaconsulting.siasaleo.service.domain.election.dto.RegisterAssignmentDtos.AssignmentDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public final class ResultDtos {
    private ResultDtos() { }
    public record VoteRow(@NotNull @Positive Long candidacyId, @NotNull @Positive Long areaId,
            @NotNull @PositiveOrZero Long voteCount, @Size(max=1024) String sourceRecordReference) { }
    // rejectedBallots <= ballotsCast is checked by the service: a cross-field
    // @AssertTrue accessor would also be serialized into every ballot response.
    public record BallotRow(@NotNull @Positive Long areaId, @NotNull @PositiveOrZero Long ballotsCast,
            @PositiveOrZero Long rejectedBallots, @Size(max=1024) String sourceRecordReference) { }
    public record CandidateRow(Long candidacyId, String candidateName, String partyName, boolean independent) { }
    // stage and coverage stay text so import fingerprints are unchanged; the
    // enums still define which values are allowed.
    public record ResultRequest(@NotNull @Positive Long contestId, @NotNull @Positive Long areaTypeId,
            @NotNull @EnumName(ResultStage.class) String stage, @NotBlank @Size(max=2048) String sourceReference,
            @NotNull @Pattern(regexp="[0-9a-f]{64}", message="must be a lowercase hex SHA-256 digest") String sourceSha256,
            @NotNull @EnumName(Coverage.class) String coverage, @Positive Long supersedesId,
            List<@NotNull @Positive Long> candidacyIds,
            List<@NotNull @Valid VoteRow> votes, List<@NotNull @Valid BallotRow> ballots) { }
    public record PublicationDto(Long id, Long contestId, Long geographySnapshotId, Long scopeAreaId,
            Long areaTypeId, ResultStage stage, String sourceReference, String sourceSha256, Coverage coverage,
            Long supersedesId, PublicationStatus status, String createdAt, String publishedAt) { }
    public record ResultImportResult(PublicationDto publication, boolean replayed, ValidationReport publicationValidation) { }
    public record CandidateTotal(Long candidacyId, String candidateName, String partyName,
            boolean independent, BigDecimal totalVotes, BigDecimal percentage, long rank) { }
    public record ResultSummary(PublicationDto publication, Long areaId, List<CandidateTotal> candidates,
            long reportedAreas, long expectedAreas, boolean complete, BigDecimal ballotsCast,
            BigDecimal rejectedBallots, AssignmentDto registerAssignment, BigDecimal registeredVoters,
            BigDecimal turnoutPercentage, String turnoutUnavailableReason) { }
}
