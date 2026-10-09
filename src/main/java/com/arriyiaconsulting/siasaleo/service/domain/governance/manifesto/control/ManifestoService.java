package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.control;

import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.mapping.ManifestoMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.control.AssessmentService;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.EvidencePolicy;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.entity.Candidacy;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.repository.CandidacyRepository;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import static com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.GovernanceRules.*;

@ApplicationScoped
public class ManifestoService {
    @Inject private ManifestoRepository manifestos;
    @Inject private ManifestoVersionRepository versions;
    @Inject private CommitmentRepository commitments;
    @Inject private CommitmentRevisionRepository revisions;
    @Inject private GoalRepository goals;
    @Inject private ManifestoCommitmentRepository versionCommitments;
    @Inject private CommitmentTargetRepository targets;
    @Inject private CommitmentGoalRepository commitmentGoals;
    @Inject private CommitmentPolicyAreaRepository policies;
    @Inject private ManifestoAttachmentRepository manifestoAttachments;
    @Inject private CommitmentAttachmentRepository commitmentAttachments;
    @Inject private PublicationEventRepository announcements;
    @Inject private UserAccountRepository accounts;
    @Inject private CandidacyRepository candidacies;
    @Inject private ManifestoMapper mapper;
    @Inject private EvidencePolicy assets;
    @Inject private AssessmentService delivery;

    @Transactional public ManifestoDto create(CreateManifestoRequest request, long actor) {
        UserAccount account = activeAccount(actor);
        Candidacy candidacy = candidacies.findById(request.candidacyId()).orElseThrow(RecordNotFoundException::new);
        if (!account.getPersonId().equals(candidacy.getPerson().getId())) throw new RecordAccessDeniedException();
        Manifesto entity = mapper.toEntity(request);
        entity.setIssuerPersonId(account.getPersonId()); entity.setCreatedByAccountId(actor);
        return mapper.toDto(manifestos.insert(entity));
    }
    private UserAccount activeAccount(long id) {
        UserAccount account = accounts.findById(id).orElseThrow(RecordAccessDeniedException::new);
        if (!account.isActive() || account.getPersonId() == null) throw new RecordAccessDeniedException();
        return account;
    }
    public Manifesto requireOwner(long id, long actor) {
        Manifesto entity = manifestos.findById(id).orElseThrow(RecordNotFoundException::new);
        if (!entity.getIssuerPersonId().equals(activeAccount(actor).getPersonId())) throw new RecordAccessDeniedException();
        return entity;
    }
    @Transactional public ManifestoVersionDto createVersion(long manifestoId, CreateManifestoVersionRequest request, long actor) {
        requireOwner(manifestoId, actor);
        ManifestoVersion entity = mapper.toEntity(request);
        entity.setManifestoId(manifestoId); entity.setVersionNumber(next(versions.latestNumber(manifestoId))); entity.setRecordedByAccountId(actor);
        return mapper.toDto(versions.insert(entity));
    }
    @Transactional public GoalDto createGoal(long manifestoId, CreateGoalRequest request, long actor) {
        requireOwner(manifestoId, actor);
        Goal entity = mapper.toEntity(request); entity.setManifestoId(manifestoId);
        return mapper.toDto(goals.insert(entity));
    }
    @Transactional public CommitmentRevisionDto createCommitment(long manifestoId, CreateCommitmentRevisionRequest request, long actor) {
        requireOwner(manifestoId, actor);
        Commitment entity = new Commitment(); entity.setManifestoId(manifestoId);
        return createRevision(commitments.insert(entity).getId(), request, actor);
    }
    @Transactional public CommitmentRevisionDto createRevision(long commitmentId, CreateCommitmentRevisionRequest request, long actor) {
        Commitment commitment = commitments.findById(commitmentId).orElseThrow(RecordNotFoundException::new);
        requireOwner(commitment.getManifestoId(), actor);
        CommitmentRevision entity = mapper.toEntity(request);
        entity.setCommitmentId(commitmentId); entity.setRevisionNumber(next(revisions.latestNumber(commitmentId))); entity.setRecordedByAccountId(actor);
        return mapper.toDto(revisions.insert(entity));
    }
    @Transactional public ManifestoCommitmentDto attachCommitment(long versionId, CommitmentLinkRequest request, long actor) {
        requireDraft(ownedVersion(versionId, actor).getPublicationStatus());
        // The database also enforces matching manifesto roots and one revision per promise.
        ownedRevision(request.commitmentRevisionId(), actor);
        ManifestoCommitment entity = new ManifestoCommitment();
        entity.setManifestoVersionId(versionId); entity.setCommitmentRevisionId(request.commitmentRevisionId());
        return mapper.toDto(versionCommitments.insert(entity));
    }
    @Transactional public CommitmentTargetDto createTarget(long revisionId, CreateCommitmentTargetRequest request, long actor) {
        requireUnsealed(ownedRevision(revisionId, actor));
        CommitmentTarget entity = mapper.toEntity(request); entity.setCommitmentRevisionId(revisionId);
        return mapper.toDto(targets.insert(entity));
    }
    @Transactional public CommitmentGoalDto attachGoal(long revisionId, GoalLinkRequest request, long actor) {
        requireUnsealed(ownedRevision(revisionId, actor));
        CommitmentGoal entity = new CommitmentGoal(); entity.setCommitmentRevisionId(revisionId); entity.setGoalId(request.goalId());
        return mapper.toDto(commitmentGoals.insert(entity));
    }
    @Transactional public CommitmentPolicyAreaDto classify(long revisionId, PolicyAreaRequest request, long actor) {
        requireUnsealed(ownedRevision(revisionId, actor));
        CommitmentPolicyArea entity = new CommitmentPolicyArea(); entity.setCommitmentRevisionId(revisionId); entity.setPolicyAreaId(request.policyAreaId());
        return mapper.toDto(policies.insert(entity));
    }
    @Transactional public ManifestoAttachmentDto attachVersionAsset(long versionId, AttachmentRequest request, long actor) {
        requireDraft(ownedVersion(versionId, actor).getPublicationStatus()); assets.requireEvidence(request.assetVersionId());
        ManifestoAttachment entity = new ManifestoAttachment(); entity.setManifestoVersionId(versionId);
        entity.setAssetVersionId(request.assetVersionId()); entity.setCaption(request.caption()); entity.setLocator(request.locator());
        return mapper.toDto(manifestoAttachments.insert(entity));
    }
    @Transactional public CommitmentAttachmentDto attachRevisionAsset(long revisionId, AttachmentRequest request, long actor) {
        requireUnsealed(ownedRevision(revisionId, actor)); assets.requireEvidence(request.assetVersionId());
        CommitmentAttachment entity = new CommitmentAttachment(); entity.setCommitmentRevisionId(revisionId);
        entity.setAssetVersionId(request.assetVersionId()); entity.setCaption(request.caption()); entity.setLocator(request.locator());
        return mapper.toDto(commitmentAttachments.insert(entity));
    }
    @Transactional public PublicationEventDto announce(long versionId, CreatePublicationEventRequest request, long actor) {
        if (ownedVersion(versionId, actor).getPublicationStatus() != PublicationStatus.PUBLISHED)
            throw new IllegalArgumentException("Announcements require a published version");
        if (request.assetVersionId() != null) assets.requireEvidence(request.assetVersionId());
        PublicationEvent entity = mapper.toEntity(request); entity.setManifestoVersionId(versionId); entity.setRecordedByAccountId(actor);
        return mapper.toDto(announcements.insert(entity));
    }
    @Transactional public ManifestoVersionDto publish(long id, long actor, boolean withdraw) {
        ManifestoVersion entity = ownedVersion(id, actor);
        entity.setPublicationStatus(transition(entity.getPublicationStatus(), withdraw));
        return mapper.publishedDto(versions.update(entity), versions.publicationTime(id));
    }

    public ManifestoDetailDto version(long id, Long actor) {
        ManifestoVersion version = actor == null ? versions.findById(id).orElseThrow(RecordNotFoundException::new) : ownedVersion(id, actor);
        if (actor == null) requirePublic(version.getPublicationStatus());
        List<CommitmentDetailDto> promises = versionCommitments.forVersion(id).stream().map(link ->
            new CommitmentDetailDto(mapper.toDto(revisions.findById(link.getCommitmentRevisionId()).orElseThrow(RecordNotFoundException::new)),
                targets.forRevision(link.getCommitmentRevisionId()).stream().map(mapper::toDto).toList())).toList();
        return new ManifestoDetailDto(mapper.toDto(version), promises, manifestoAttachments.forVersion(id).stream().map(mapper::toDto).toList());
    }
    public CommitmentDeliveryDto timeline(long revisionId) {
        if (revisions.publicVersionCount(revisionId, PublicationStatus.DRAFT) == 0) throw new RecordNotFoundException();
        return new CommitmentDeliveryDto(revisionId, delivery.deliveryForRevision(revisionId), delivery.assessmentsForRevision(revisionId));
    }
    public List<ManifestoVersionDto> list(long person, int pageNumber, int size) {
        return versions.forPerson(person, PublicationStatus.PUBLISHED, page(pageNumber, size)).stream().map(mapper::toDto).toList();
    }
    private ManifestoVersion ownedVersion(long id, long actor) {
        ManifestoVersion entity = versions.findById(id).orElseThrow(RecordNotFoundException::new);
        requireOwner(entity.getManifestoId(), actor); return entity;
    }
    private CommitmentRevision ownedRevision(long id, long actor) {
        CommitmentRevision entity = revisions.findById(id).orElseThrow(RecordNotFoundException::new);
        Commitment parent = commitments.findById(entity.getCommitmentId()).orElseThrow(RecordNotFoundException::new);
        requireOwner(parent.getManifestoId(), actor); return entity;
    }
    private void requireUnsealed(CommitmentRevision entity) {
        if (entity.getSealedAt() != null) throw new IllegalArgumentException("Published promises require a new revision");
    }

    @Transactional public ManifestoVersionDto editVersion(long id, PatchManifestoVersionRequest request, long actor) {
        request.requireChanges();
        ManifestoVersion entity = ownedVersion(id, actor);
        requireDraft(entity.getPublicationStatus());
        if (request.has("title")) entity.setTitle(request.getTitle());
        if (request.has("language")) entity.setLanguage(request.getLanguage());
        if (request.has("content")) entity.setContent(request.getContent());
        return mapper.toDto(versions.update(entity));
    }

    @Transactional public CommitmentRevisionDto editRevision(long id, PatchCommitmentRevisionRequest request, long actor) {
        request.requireChanges();
        CommitmentRevision entity = ownedRevision(id, actor);
        requireUnsealed(entity);
        if (request.has("wording")) entity.setWording(request.getWording());
        if (request.has("conditions")) entity.setConditions(request.getConditions());
        if (request.has("periodStart")) entity.setPeriodStart(request.getPeriodStart());
        if (request.has("periodEnd")) entity.setPeriodEnd(request.getPeriodEnd());
        if (request.has("datePrecision")) entity.setDatePrecision(request.getDatePrecision());
        if (request.has("scopeId")) entity.setScopeId(request.getScopeId());
        return mapper.toDto(revisions.update(entity));
    }
}
