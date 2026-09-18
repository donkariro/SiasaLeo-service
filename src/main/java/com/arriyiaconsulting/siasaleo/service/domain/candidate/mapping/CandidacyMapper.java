package com.arriyiaconsulting.siasaleo.service.domain.candidate.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.entity.Contest;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MappingConfig.class)
public interface CandidacyMapper {

    @Mapping(target = "personId", source = "person.id")
    @Mapping(target = "firstName", source = "person.firstName")
    @Mapping(target = "lastName", source = "person.lastName")
    @Mapping(target = "politicalPartyId", source = "politicalParty.id")
    @Mapping(target = "partyName", source = "politicalParty.name")
    @Mapping(target = "partyAbbreviation", source = "politicalParty.abbreviation")
    @Mapping(target = "status", source = "status.statusName")
    CandidacyDto toCandidacyDto(Candidacy entity);

    @Mapping(target = "person", source = "person")
    @Mapping(target = "contest", source = "contest")
    @Mapping(target = "politicalParty", source = "politicalParty")
    @Mapping(target = "status", source = "status")
    Candidacy toEntity(Person person, Contest contest, PoliticalParty politicalParty, CandidacyStatus status);
}
