package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.dto.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.*;

@Mapper(config = MappingConfig.class)
public interface GovernanceCatalogMapper {
    PolicyAreaDto toDto(PolicyArea entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "displayOrder", source = "displayOrder", defaultValue = "0")
    PolicyArea toEntity(CreatePolicyAreaRequest request);

    UnitOfMeasureDto toDto(UnitOfMeasure entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    UnitOfMeasure toEntity(CreateUnitOfMeasureRequest request);

    IndicatorDto toDto(Indicator entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "unit", source = "unit")
    @Mapping(target = "unitId", source = "unitId")
    @Mapping(target = "methodology", source = "methodology")
    @Mapping(target = "direction", source = "direction")
    @Mapping(target = "description", source = "description")
    Indicator toEntity(CreateIndicatorRequest request);

    MeasurementScopeDto toDto(MeasurementScope entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "kind", source = "kind")
    @Mapping(target = "snapshotId", source = "snapshotId")
    @Mapping(target = "areaId", source = "areaId")
    @Mapping(target = "population", source = "population")
    MeasurementScope toEntity(CreateMeasurementScopeRequest request);

    EvidenceRelationshipTypeDto toDto(EvidenceRelationshipType entity);

}
