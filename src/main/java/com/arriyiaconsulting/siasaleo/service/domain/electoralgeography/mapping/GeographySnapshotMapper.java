package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MappingConfig.class)
public interface GeographySnapshotMapper {
    GeographySnapshotDto toDto(ElectoralGeographySnapshot snapshot);
    @Mapping(target = "areaType", source = "areaType.name")
    ElectoralAreaSnapshotDto toDto(ElectoralAreaSnapshot area);
    AreaCorrespondenceDto toDto(ElectoralAreaCorrespondence correspondence);

    @Mapping(target = "supersedesSnapshotId", source = "predecessorId")
    ElectoralGeographySnapshot toEntity(CreateGeographySnapshotRequest request, Long predecessorId);

    @Mapping(target = "snapshotId", source = "snapshotId")
    @Mapping(target = "areaType", source = "type")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "areaCode", source = "request.areaCode")
    @Mapping(target = "parentId", source = "request.parentId")
    @Mapping(target = "ancestorPath", source = "path")
    @Mapping(target = "sourceRecordReference", source = "request.sourceRecordReference")
    ElectoralAreaSnapshot toEntity(CreateSnapshotAreaRequest request, Long snapshotId, AreaType type, String path);
}
