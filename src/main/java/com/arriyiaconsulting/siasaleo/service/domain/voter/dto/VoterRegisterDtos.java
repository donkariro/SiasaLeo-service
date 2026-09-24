package com.arriyiaconsulting.siasaleo.service.domain.voter.dto;
import java.math.BigDecimal;
import java.util.List;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport.ValidationReport;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.Coverage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.validation.EnumName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public final class VoterRegisterDtos {
    private VoterRegisterDtos() { }
    // Zero registered voters is a real count; null means unknown and is rejected.
    public record RegisterCount(@NotNull @Positive Long areaId, @NotNull @PositiveOrZero Long registeredVoters,
            @Size(max=1024) String sourceRecordReference) { }
    // coverage stays text so import fingerprints are unchanged; the enum still
    // defines which values are allowed.
    public record RegisterRequest(@NotNull @Positive Long geographySnapshotId, @NotNull @Positive Long scopeAreaId,
            @NotNull @Positive Long areaTypeId, @NotBlank @Size(max=2048) String sourceReference,
            @NotNull @Pattern(regexp="[0-9a-f]{64}", message="must be a lowercase hex SHA-256 digest") String sourceSha256,
            @NotNull @EnumName(Coverage.class) String coverage, @Positive Long supersedesId,
            List<@NotNull @Valid RegisterCount> counts) { }
    public record RegisterDto(Long id, Long geographySnapshotId, Long scopeAreaId, Long areaTypeId,
            String sourceReference, String sourceSha256, Coverage coverage, Long supersedesId,
            PublicationStatus status, String createdAt, String publishedAt) { }
    public record RegisterImportResult(RegisterDto register, boolean replayed, ValidationReport publicationValidation) { }
    public record RegisterSummary(RegisterDto register, Long areaId, BigDecimal registeredVoters,
            long reportedAreas, long expectedAreas, boolean complete) { }
}
