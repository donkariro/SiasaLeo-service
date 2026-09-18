package com.arriyiaconsulting.siasaleo.service.domain.election.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.election.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.entity.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MappingConfig.class)
public interface ElectionMapper {

    ContestDto toContestDto(Contest entity);

    ElectionCycleDto toElectionCycleDto(ElectionCycle entity);

    @Mapping(target = "electionCycleId", source = "electionCycle.id")
    ElectionEventDto toElectionEventDto(ElectionEvent entity);

    ElectionStatusDto toElectionStatusDto(ElectionStatus entity);

    ElectionTypeDto toElectionTypeDto(ElectionType entity);

    @Mapping(target = "electionCycle", source = "electionCycle")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "electionDate", source = "request.electionDate")
    ElectionEvent toEntity(CreateElectionEventRequest request, ElectionCycle electionCycle,
            ElectionType type, ElectionStatus status);

    @Mapping(target = "electionEventId", source = "request.electionEventId")
    @Mapping(target = "seatId", source = "request.seatId")
    @Mapping(target = "description", source = "description")
    Contest toEntity(CreateContestRequest request, String description);
}
