package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.*;

@Mapper(config = MappingConfig.class)
public interface ManifestoMapper {
    ManifestoDto toDto(Manifesto entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "candidacyId", source = "candidacyId")
    Manifesto toEntity(CreateManifestoRequest request);

    ManifestoVersionDto toDto(ManifestoVersion entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "language", source = "language", defaultValue = "en")
    @Mapping(target = "content", source = "content")
    ManifestoVersion toEntity(CreateManifestoVersionRequest request);

    GoalDto toDto(Goal entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "description", source = "description")
    Goal toEntity(CreateGoalRequest request);

    CommitmentDto toDto(Commitment entity);

    CommitmentRevisionDto toDto(CommitmentRevision entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "wording", source = "wording")
    @Mapping(target = "conditions", source = "conditions")
    @Mapping(target = "periodStart", source = "periodStart")
    @Mapping(target = "periodEnd", source = "periodEnd")
    @Mapping(target = "datePrecision", source = "datePrecision", defaultValue = "UNKNOWN")
    @Mapping(target = "scopeId", source = "scopeId")
    CommitmentRevision toEntity(CreateCommitmentRevisionRequest request);

    ManifestoCommitmentDto toDto(ManifestoCommitment entity);

    CommitmentGoalDto toDto(CommitmentGoal entity);

    CommitmentPolicyAreaDto toDto(CommitmentPolicyArea entity);

    CommitmentTargetDto toDto(CommitmentTarget entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "indicatorId", source = "indicatorId")
    @Mapping(target = "scopeId", source = "scopeId")
    @Mapping(target = "baselineValue", source = "baselineValue")
    @Mapping(target = "targetValue", source = "targetValue")
    @Mapping(target = "periodStart", source = "periodStart")
    @Mapping(target = "periodEnd", source = "periodEnd")
    CommitmentTarget toEntity(CreateCommitmentTargetRequest request);

    ManifestoAttachmentDto toDto(ManifestoAttachment entity);

    CommitmentAttachmentDto toDto(CommitmentAttachment entity);

    PublicationEventDto toDto(PublicationEvent entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "announcedAt", source = "announcedAt")
    @Mapping(target = "venue", source = "venue")
    @Mapping(target = "assetVersionId", source = "assetVersionId")
    PublicationEvent toEntity(CreatePublicationEventRequest request);

    @Mapping(target = "publishedAt", source = "publishedAt")
    ManifestoVersionDto publishedDto(ManifestoVersion entity, java.time.OffsetDateTime publishedAt);

}
