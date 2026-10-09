package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.control;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.mapping.PerformanceMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.EvidencePolicy;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import static com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.GovernanceRules.*;

/** Independent observations, the performance targets they are compared against, and those comparisons. */
@ApplicationScoped
public class ObservationService {
    @Inject private ObservationRepository observations;
    @Inject private PerformanceTargetRepository targets;
    @Inject private TargetObservationRepository comparisons;
    @Inject private ObservationEvidenceRepository observationEvidence;
    @Inject private PerformanceMapper mapper;
    @Inject private EvidencePolicy assets;

    @Transactional public ObservationDto createObservation(CreateObservationRequest request, long actor) {
        Observation entity = mapper.toEntity(request);
        entity.setRecordedByAccountId(actor);
        return mapper.toDto(observations.insert(entity));
    }
    @Transactional public PerformanceTargetDto createTarget(CreatePerformanceTargetRequest request) { return mapper.toDto(targets.insert(mapper.toEntity(request))); }
    @Transactional public TargetObservationDto compare(CreateTargetObservationRequest request) { return mapper.toDto(comparisons.insert(mapper.toEntity(request))); }
    @Transactional public ObservationEvidenceDto addObservationEvidence(long id, EvidenceRequest request) {
        requireDraft(observation(id).getPublicationStatus()); assets.requireEvidence(request.assetVersionId());
        ObservationEvidence entity = new ObservationEvidence();
        entity.setObservationId(id); entity.setAssetVersionId(request.assetVersionId());
        entity.setRelationshipCode(request.relationshipCode()); entity.setLocator(request.locator()); entity.setCaption(request.caption());
        return mapper.toDto(observationEvidence.insert(entity));
    }
    @Transactional public ObservationDto publishObservation(long id, boolean withdraw) {
        Observation entity = observation(id);
        entity.setPublicationStatus(transition(entity.getPublicationStatus(), withdraw));
        return mapper.publishedDto(observations.update(entity), observations.publicationTime(id));
    }
    public ObservationDto readObservation(long id, boolean editor) {
        Observation entity = observation(id); if (!editor) requirePublic(entity.getPublicationStatus()); return mapper.toDto(entity);
    }
    private Observation observation(long id) { return observations.findById(id).orElseThrow(RecordNotFoundException::new); }

    @Transactional public ObservationDto editObservation(long id, PatchObservationRequest request) {
        request.requireChanges();
        Observation entity = observation(id);
        requireDraft(entity.getPublicationStatus());
        if (request.has("indicatorId")) entity.setIndicatorId(request.getIndicatorId());
        if (request.has("actualValue")) entity.setActualValue(request.getActualValue());
        if (request.has("qualitativeResult")) entity.setQualitativeResult(request.getQualitativeResult());
        if (request.has("officeholderId")) entity.setOfficeholderId(request.getOfficeholderId());
        if (request.has("workEffortId")) entity.setWorkEffortId(request.getWorkEffortId());
        if (request.has("scopeId")) entity.setScopeId(request.getScopeId());
        if (request.has("periodStart")) entity.setPeriodStart(request.getPeriodStart());
        if (request.has("periodEnd")) entity.setPeriodEnd(request.getPeriodEnd());
        if (request.has("datePrecision")) entity.setDatePrecision(request.getDatePrecision());
        if (request.has("sourceReference")) entity.setSourceReference(request.getSourceReference());
        return mapper.toDto(observations.update(entity));
    }
}
