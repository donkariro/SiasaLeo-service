package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.control;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.mapping.PerformanceMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control.DigitalAssetService;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import static com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.GovernanceRules.*;

/**
 * Reviewed assessments of delivery, and the links that tie work to a promise.
 * A delivery link records relevance only; whether a promise was kept is an
 * assessment, so both live here.
 */
@ApplicationScoped
public class AssessmentService {
    @Inject private AssessmentRepository assessments;
    @Inject private AssessmentEvidenceRepository assessmentEvidence;
    @Inject private CommitmentDeliveryLinkRepository deliveryLinks;
    @Inject private WorkEffortRepository works;
    @Inject private WorkEffortParticipationRepository participations;
    @Inject private PerformanceMapper mapper;
    @Inject private DigitalAssetService assets;

    @Transactional public CommitmentDeliveryLinkDto createDeliveryLink(CreateCommitmentDeliveryLinkRequest request, long actor) {
        CommitmentDeliveryLink entity = mapper.toEntity(request);
        entity.setRecordedByAccountId(actor);
        return mapper.toDto(deliveryLinks.insert(entity));
    }
    @Transactional public AssessmentDto createAssessment(CreateAssessmentRequest request, long actor) {
        Assessment entity = mapper.toEntity(request);
        entity.setReviewedByAccountId(actor);
        return mapper.toDto(assessments.insert(entity));
    }
    @Transactional public AssessmentEvidenceDto addAssessmentEvidence(long id, EvidenceRequest request) {
        requireDraft(assessment(id).getPublicationStatus()); assets.requireSealed(request.assetVersionId());
        AssessmentEvidence entity = new AssessmentEvidence();
        entity.setAssessmentId(id); entity.setAssetVersionId(request.assetVersionId());
        entity.setRelationshipCode(request.relationshipCode()); entity.setLocator(request.locator()); entity.setCaption(request.caption());
        return mapper.toDto(assessmentEvidence.insert(entity));
    }
    @Transactional public AssessmentDto publishAssessment(long id, boolean withdraw) {
        Assessment entity = assessment(id);
        entity.setPublicationStatus(transition(entity.getPublicationStatus(), withdraw));
        return mapper.publishedDto(assessments.update(entity), assessments.publicationTime(id));
    }
    public AssessmentDto readAssessment(long id, boolean editor) {
        Assessment entity = assessment(id); if (!editor) requirePublic(entity.getPublicationStatus()); return mapper.toDto(entity);
    }
    public List<AssessmentDetailDto> publishedForRecord(long recordId) {
        return assessments.forRecord(recordId, PublicationStatus.PUBLISHED).stream().map(this::detail).toList();
    }
    public List<AssessmentDetailDto> assessmentsForRevision(long id) {
        return assessments.forRevision(id, PublicationStatus.PUBLISHED).stream().map(this::detail).toList();
    }
    public List<DeliveryLinkDetailDto> deliveryForRevision(long id) {
        return deliveryLinks.forRevision(id).stream().map(link -> new DeliveryLinkDetailDto(mapper.toDto(link),
            mapper.toDto(works.findById(link.getWorkEffortId()).orElseThrow(RecordNotFoundException::new)),
            participations.forWork(link.getWorkEffortId()).stream().map(mapper::toDto).toList())).toList();
    }
    private AssessmentDetailDto detail(Assessment entity) {
        return new AssessmentDetailDto(mapper.toDto(entity), assessmentEvidence.forAssessment(entity.getId()).stream().map(mapper::toDto).toList());
    }
    private Assessment assessment(long id) { return assessments.findById(id).orElseThrow(RecordNotFoundException::new); }

    @Transactional public AssessmentDto editAssessment(long id, PatchAssessmentRequest request) {
        request.requireChanges();
        Assessment entity = assessment(id);
        requireDraft(entity.getPublicationStatus());
        if (request.has("recordId")) entity.setRecordId(request.getRecordId());
        if (request.has("commitmentRevisionId")) entity.setCommitmentRevisionId(request.getCommitmentRevisionId());
        if (request.has("commitmentTargetId")) entity.setCommitmentTargetId(request.getCommitmentTargetId());
        if (request.has("observationId")) entity.setObservationId(request.getObservationId());
        if (request.has("deliveryStatus")) entity.setDeliveryStatus(request.getDeliveryStatus());
        if (request.has("evidenceStatus")) entity.setEvidenceStatus(request.getEvidenceStatus());
        if (request.has("criteria")) entity.setCriteria(request.getCriteria());
        if (request.has("rationale")) entity.setRationale(request.getRationale());
        return mapper.toDto(assessments.update(entity));
    }
}
