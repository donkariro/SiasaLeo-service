package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MappingConfig.class)
public interface ElectoralAreaMapper {

    AreaTypeDto toAreaTypeDto(AreaType entity);

    @Mapping(target = "areaType", source = "areaType.name")
    ElectoralAreaDto toElectoralAreaDto(ElectoralArea entity);

    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "areaCode", source = "request.areaCode")
    @Mapping(target = "areaType", source = "areaType")
    @Mapping(target = "parent", source = "parent")
    @Mapping(target = "ancestorPath", source = "ancestorPath")
    ElectoralArea toEntity(CreateElectoralAreaRequest request, AreaType areaType,
            ElectoralArea parent, String ancestorPath);
}
