package com.arriyiaconsulting.siasaleo.service.domain.governance.performance.control;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.mapping.PerformanceMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.EvidencePolicy;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import static com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.GovernanceRules.*;

/** Work efforts, who took part in them, and the public contribution history built from both. */
@ApplicationScoped
public class WorkEffortService {
    @Inject private WorkEffortRepository works;
    @Inject private WorkEffortParticipationRepository participations;
    @Inject private WorkEffortAssetRepository workAssets;
    @Inject private WorkEffortPolicyAreaRepository workPolicies;
    @Inject private PerformanceMapper mapper;
    @Inject private EvidencePolicy assets;

    @Transactional public WorkEffortDto createWork(CreateWorkEffortRequest request, long actor) {
        WorkEffort entity = mapper.toEntity(request);
        entity.setRecordedByAccountId(actor);
        return mapper.toDto(works.insert(entity));
    }
    @Transactional public WorkEffortParticipationDto createParticipation(CreateWorkEffortParticipationRequest request, long actor) {
        assets.requireEvidence(request.assetVersionId());
        WorkEffortParticipation entity = mapper.toEntity(request);
        entity.setRecordedByAccountId(actor);
        return mapper.toDto(participations.insert(entity));
    }
    @Transactional public WorkEffortAssetDto addWorkAsset(long id, AttachmentRequest request) {
        assets.requireEvidence(request.assetVersionId());
        WorkEffortAsset entity = new WorkEffortAsset();
        entity.setWorkEffortId(id); entity.setAssetVersionId(request.assetVersionId());
        entity.setLocator(request.locator()); entity.setCaption(request.caption());
        return mapper.toDto(workAssets.insert(entity));
    }
    @Transactional public WorkEffortPolicyAreaDto classifyWork(long id, long policy) {
        WorkEffortPolicyArea entity = new WorkEffortPolicyArea(); entity.setWorkEffortId(id); entity.setPolicyAreaId(policy);
        return mapper.toDto(workPolicies.insert(entity));
    }
    public List<ContributionDto> contributions(long person, int pageNumber, int size) {
        return participations.publicContributions(person, PublicationStatus.PUBLISHED, page(pageNumber, size)).stream()
            .map(p -> new ContributionDto(mapper.toDto(p), mapper.toDto(works.findById(p.getWorkEffortId()).orElseThrow(RecordNotFoundException::new)))).toList();
    }
}
