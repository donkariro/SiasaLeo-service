package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.*;

@Mapper(config = MappingConfig.class)
public interface PerformanceMapper {
    WorkEffortTypeDto toDto(WorkEffortType entity);

    ParticipationRoleTypeDto toDto(ParticipationRoleType entity);

    WorkEffortDto toDto(WorkEffort entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "typeCode", source = "typeCode")
    @Mapping(target = "periodStart", source = "periodStart")
    @Mapping(target = "periodEnd", source = "periodEnd")
    @Mapping(target = "datePrecision", source = "datePrecision", defaultValue = "UNKNOWN")
    @Mapping(target = "scopeId", source = "scopeId")
    WorkEffort toEntity(CreateWorkEffortRequest request);

    WorkEffortPolicyAreaDto toDto(WorkEffortPolicyArea entity);

    WorkEffortAssetDto toDto(WorkEffortAsset entity);

    WorkEffortParticipationDto toDto(WorkEffortParticipation entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "workEffortId", source = "workEffortId")
    @Mapping(target = "partyId", source = "partyId")
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "officeholderId", source = "officeholderId")
    @Mapping(target = "periodStart", source = "periodStart")
    @Mapping(target = "periodEnd", source = "periodEnd")
    @Mapping(target = "datePrecision", source = "datePrecision", defaultValue = "UNKNOWN")
    @Mapping(target = "assetVersionId", source = "assetVersionId")
    @Mapping(target = "locator", source = "locator")
    WorkEffortParticipation toEntity(CreateWorkEffortParticipationRequest request);

    AchievementClaimDto toDto(AchievementClaim entity);

    ClaimRevisionDto toDto(ClaimRevision entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "claimId", source = "claimId")
    @Mapping(target = "officeholderId", source = "officeholderId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "claimantPartyId", source = "claimantPartyId")
    @Mapping(target = "workEffortId", source = "workEffortId")
    @Mapping(target = "scopeId", source = "scopeId")
    @Mapping(target = "periodStart", source = "periodStart")
    @Mapping(target = "periodEnd", source = "periodEnd")
    @Mapping(target = "datePrecision", source = "datePrecision", defaultValue = "DAY")
    @Mapping(target = "sourceReference", source = "sourceReference")
    ClaimRevision toEntity(CreateClaimRevisionRequest request);

    ClaimEvidenceDto toDto(ClaimEvidence entity);

    ClaimPolicyAreaDto toDto(ClaimPolicyArea entity);

    PerformanceTargetDto toDto(PerformanceTarget entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "officeholderId", source = "officeholderId")
    @Mapping(target = "indicatorId", source = "indicatorId")
    @Mapping(target = "scopeId", source = "scopeId")
    @Mapping(target = "periodStart", source = "periodStart")
    @Mapping(target = "periodEnd", source = "periodEnd")
    @Mapping(target = "baselineValue", source = "baselineValue")
    @Mapping(target = "targetValue", source = "targetValue")
    @Mapping(target = "sourceReference", source = "sourceReference")
    @Mapping(target = "notes", source = "notes")
    PerformanceTarget toEntity(CreatePerformanceTargetRequest request);

    ObservationDto toDto(Observation entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "indicatorId", source = "indicatorId")
    @Mapping(target = "actualValue", source = "actualValue")
    @Mapping(target = "qualitativeResult", source = "qualitativeResult")
    @Mapping(target = "officeholderId", source = "officeholderId")
    @Mapping(target = "workEffortId", source = "workEffortId")
    @Mapping(target = "scopeId", source = "scopeId")
    @Mapping(target = "periodStart", source = "periodStart")
    @Mapping(target = "periodEnd", source = "periodEnd")
    @Mapping(target = "datePrecision", source = "datePrecision", defaultValue = "UNKNOWN")
    @Mapping(target = "sourceReference", source = "sourceReference")
    Observation toEntity(CreateObservationRequest request);

    ObservationEvidenceDto toDto(ObservationEvidence entity);

    TargetObservationDto toDto(TargetObservation entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "targetId", source = "targetId")
    @Mapping(target = "observationId", source = "observationId")
    TargetObservation toEntity(CreateTargetObservationRequest request);

    CommitmentDeliveryLinkDto toDto(CommitmentDeliveryLink entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "commitmentRevisionId", source = "commitmentRevisionId")
    @Mapping(target = "workEffortId", source = "workEffortId")
    @Mapping(target = "relationshipType", source = "relationshipType")
    @Mapping(target = "rationale", source = "rationale")
    CommitmentDeliveryLink toEntity(CreateCommitmentDeliveryLinkRequest request);

    AssessmentDto toDto(Assessment entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "recordId", source = "recordId")
    @Mapping(target = "commitmentRevisionId", source = "commitmentRevisionId")
    @Mapping(target = "commitmentTargetId", source = "commitmentTargetId")
    @Mapping(target = "observationId", source = "observationId")
    @Mapping(target = "deliveryStatus", source = "deliveryStatus")
    @Mapping(target = "evidenceStatus", source = "evidenceStatus")
    @Mapping(target = "criteria", source = "criteria")
    @Mapping(target = "rationale", source = "rationale")
    Assessment toEntity(CreateAssessmentRequest request);

    AssessmentEvidenceDto toDto(AssessmentEvidence entity);

    @Mapping(target = "publishedAt", source = "publishedAt")
    ClaimRevisionDto publishedDto(ClaimRevision entity, java.time.OffsetDateTime publishedAt);

    @Mapping(target = "publishedAt", source = "publishedAt")
    ObservationDto publishedDto(Observation entity, java.time.OffsetDateTime publishedAt);

    @Mapping(target = "publishedAt", source = "publishedAt")
    AssessmentDto publishedDto(Assessment entity, java.time.OffsetDateTime publishedAt);

}
