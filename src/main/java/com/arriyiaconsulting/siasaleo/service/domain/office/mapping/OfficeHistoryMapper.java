package com.arriyiaconsulting.siasaleo.service.domain.office.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.office.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.office.dto.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.*;

@Mapper(config = MappingConfig.class)
public interface OfficeHistoryMapper {
    OfficeholderDto toDto(Officeholder entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "seatId", source = "seatId")
    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "termId", source = "termId")
    @Mapping(target = "candidacyId", source = "candidacyId")
    @Mapping(target = "entryReasonId", source = "entryReasonId")
    @Mapping(target = "exitReasonId", source = "exitReasonId")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    Officeholder toEntity(CreateOfficeholderRequest request);

    LeadershipTermDto toDto(LeadershipTerm entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "seatId", source = "seatId")
    @Mapping(target = "cycleId", source = "cycleId")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    LeadershipTerm toEntity(CreateLeadershipTermRequest request);

    OfficeholderEvidenceDto toDto(OfficeholderEvidence entity);

}
