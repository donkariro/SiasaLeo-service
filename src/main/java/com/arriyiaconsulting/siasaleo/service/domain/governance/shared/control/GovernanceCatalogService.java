package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control;

import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.mapping.GovernanceCatalogMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.mapping.PerformanceMapper;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository.DigitalAssetTypeRepository;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.DigitalAssetTypeDto;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.mapping.DigitalAssetMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class GovernanceCatalogService {
    @Inject private PolicyAreaRepository policies;
    @Inject private UnitOfMeasureRepository units;
    @Inject private IndicatorRepository indicators;
    @Inject private MeasurementScopeRepository scopes;
    @Inject private EvidenceRelationshipTypeRepository evidenceTypes;
    @Inject private WorkEffortTypeRepository workTypes;
    @Inject private ParticipationRoleTypeRepository roles;
    @Inject private DigitalAssetTypeRepository assetTypes;
    @Inject private GovernanceCatalogMapper mapper;
    @Inject private PerformanceMapper performanceMapper;
    @Inject private DigitalAssetMapper assetMapper;

    public List<PolicyAreaDto> policies() { return policies.ordered().stream().map(mapper::toDto).toList(); }
    public List<UnitOfMeasureDto> units() { return units.ordered().stream().map(mapper::toDto).toList(); }
    public List<IndicatorDto> indicators() { return indicators.ordered().stream().map(mapper::toDto).toList(); }
    public List<WorkEffortTypeDto> workTypes() { return workTypes.ordered().stream().map(performanceMapper::toDto).toList(); }
    public List<ParticipationRoleTypeDto> roles() { return roles.ordered().stream().map(performanceMapper::toDto).toList(); }
    public List<DigitalAssetTypeDto> assetTypes() { return assetTypes.ordered().stream().map(assetMapper::toDto).toList(); }
    public List<EvidenceRelationshipTypeDto> evidenceTypes() { return evidenceTypes.ordered().stream().map(mapper::toDto).toList(); }
    @Transactional public PolicyAreaDto createPolicy(CreatePolicyAreaRequest request) { return mapper.toDto(policies.insert(mapper.toEntity(request))); }
    @Transactional public UnitOfMeasureDto createUnit(CreateUnitOfMeasureRequest request) { return mapper.toDto(units.insert(mapper.toEntity(request))); }
    @Transactional public MeasurementScopeDto createScope(CreateMeasurementScopeRequest request) { return mapper.toDto(scopes.insert(mapper.toEntity(request))); }
    @Transactional public IndicatorDto createIndicator(CreateIndicatorRequest request) {
        UnitOfMeasure unit = units.findByName(request.unit()).orElseThrow(() -> new IllegalArgumentException("Unknown unit"));
        if (request.unitId() != null && !request.unitId().equals(unit.getId())) throw new IllegalArgumentException("Unit ID and name must agree");
        Indicator indicator = mapper.toEntity(request);
        indicator.setUnitId(unit.getId());
        return mapper.toDto(indicators.insert(indicator));
    }
}
