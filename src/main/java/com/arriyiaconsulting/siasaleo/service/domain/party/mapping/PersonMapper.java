package com.arriyiaconsulting.siasaleo.service.domain.party.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.party.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MappingConfig.class)
public interface PersonMapper {

    ClaimablePersonDto toClaimablePersonDto(Person entity);

    @Mapping(target = "personId", source = "person.id")
    @Mapping(target = "firstName", source = "person.firstName")
    @Mapping(target = "lastName", source = "person.lastName")
    PersonClaimDto toPersonClaimDto(PersonClaim entity);

    ProfileDetails toProfileDetails(Person person);

    @Mapping(target = "firstName", expression = "java(details.firstName().trim())")
    @Mapping(target = "lastName", expression = "java(details.lastName().trim())")
    Person toEntity(ProfileDetails details);
}
