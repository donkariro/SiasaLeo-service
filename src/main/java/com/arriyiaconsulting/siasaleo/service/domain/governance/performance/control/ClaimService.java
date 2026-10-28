package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.control;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.mapping.PerformanceMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.domain.office.repository.OfficeholderRepository;
import com.arriyiaconsulting.siasaleo.service.domain.office.mapping.OfficeHistoryMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.EvidencePolicy;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import static com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.GovernanceRules.*;

/** Achievement claims and their revisions, and the public track record built from them. */
@ApplicationScoped
public class ClaimService {
    @Inject private AchievementClaimRepository claims;
    @Inject private ClaimRevisionRepository revisions;
    @Inject private ClaimEvidenceRepository claimEvidence;
    @Inject private ClaimPolicyAreaRepository claimPolicies;
    @Inject private OfficeholderRepository officeholders;
    @Inject private OfficeHistoryMapper officeMapper;
    @Inject private PerformanceMapper mapper;
    @Inject private EvidencePolicy assets;
    @Inject private AssessmentService assessments;
    @Inject private NativeQueries locks;

    @Transactional public ClaimRevisionDto createClaim(CreateClaimRevisionRequest request, long actor) {
        AchievementClaim claim;
        if (request.claimId() == null) {
            claim = new AchievementClaim();
            claim.setOfficeholderId(request.officeholderId());
            claim = claims.insert(claim);
        } else {
            claim = locks.lock(AchievementClaim.class, request.claimId()).orElseThrow(RecordNotFoundException::new);
            if (!claim.getOfficeholderId().equals(request.officeholderId()))
                throw new IllegalArgumentException("A claim cannot be moved to another tenure");
        }
        ClaimRevision revision = mapper.toEntity(request);
        revision.setClaimId(claim.getId());
        revision.setRevisionNumber(next(revisions.latestNumber(claim.getId())));
        revision.setRecordedByAccountId(actor);
        return mapper.toDto(revisions.insert(revision));
    }
    @Transactional public ClaimEvidenceDto addClaimEvidence(long id, EvidenceRequest request) {
        requireDraft(claim(id).getPublicationStatus()); assets.requireEvidence(request.assetVersionId());
        ClaimEvidence entity = new ClaimEvidence();
        entity.setRecordId(id); entity.setAssetVersionId(request.assetVersionId());
        entity.setRelationshipCode(request.relationshipCode()); entity.setLocator(request.locator()); entity.setCaption(request.caption());
        return mapper.toDto(claimEvidence.insert(entity));
    }
    @Transactional public ClaimPolicyAreaDto classifyClaim(long id, long policy) {
        requireDraft(claim(id).getPublicationStatus());
        ClaimPolicyArea entity = new ClaimPolicyArea(); entity.setRecordId(id); entity.setPolicyAreaId(policy);
        return mapper.toDto(claimPolicies.insert(entity));
    }
    @Transactional public ClaimRevisionDto publishClaim(long id, boolean withdraw) {
        ClaimRevision entity = claim(id);
        entity.setPublicationStatus(transition(entity.getPublicationStatus(), withdraw));
        return mapper.publishedDto(revisions.update(entity), revisions.publicationTime(id));
    }
    public ClaimRevisionDto readClaim(long id, boolean editor) {
        ClaimRevision entity = claim(id); if (!editor) requirePublic(entity.getPublicationStatus()); return mapper.toDto(entity);
    }
    public List<TrackRecordEntryDto> trackRecord(long person, int pageNumber, int size) {
        return revisions.forPerson(person, PublicationStatus.PUBLISHED, page(pageNumber, size)).stream().map(revision ->
            new TrackRecordEntryDto(mapper.toDto(revision), officeMapper.toDto(officeholders.findById(revision.getOfficeholderId()).orElseThrow(RecordNotFoundException::new)),
                claimEvidence.forRecord(revision.getId()).stream().map(mapper::toDto).toList(),
                assessments.publishedForRecord(revision.getId()))).toList();
    }
    private ClaimRevision claim(long id) { return revisions.findById(id).orElseThrow(RecordNotFoundException::new); }

    @Transactional public ClaimRevisionDto editClaim(long id, PatchClaimRevisionRequest request) {
        request.requireChanges();
        ClaimRevision entity = claim(id);
        requireDraft(entity.getPublicationStatus());
        if (request.has("title")) entity.setTitle(request.getTitle());
        if (request.has("description")) entity.setDescription(request.getDescription());
        if (request.has("periodStart")) entity.setPeriodStart(request.getPeriodStart());
        if (request.has("periodEnd")) entity.setPeriodEnd(request.getPeriodEnd());
        if (request.has("sourceReference")) entity.setSourceReference(request.getSourceReference());
        if (request.has("claimantPartyId")) entity.setClaimantPartyId(request.getClaimantPartyId());
        if (request.has("workEffortId")) entity.setWorkEffortId(request.getWorkEffortId());
        if (request.has("datePrecision")) entity.setDatePrecision(request.getDatePrecision());
        if (request.has("scopeId")) entity.setScopeId(request.getScopeId());
        return mapper.toDto(revisions.update(entity));
    }
}
