package com.arriyiaconsulting.siasaleo.service.domain.voter.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralArea;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.voter.entity.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MappingConfig.class)
public interface VoterMapper {

    @Mapping(target = "personId", source = "person.id")
    @Mapping(target = "firstName", source = "person.firstName")
    @Mapping(target = "lastName", source = "person.lastName")
    @Mapping(target = "registrationCenterId", source = "registrationCenter.id")
    @Mapping(target = "registrationCenterName", source = "registrationCenter.name")
    VoterRegistrationDto toVoterRegistrationDto(VoterRegistration entity);

    @Mapping(target = "person", source = "person")
    @Mapping(target = "registrationCenter", source = "registrationCenter")
    @Mapping(target = "registrationDate", source = "registrationDate")
    VoterRegistration toEntity(Person person, ElectoralArea registrationCenter, LocalDate registrationDate);
}
