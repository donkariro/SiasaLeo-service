package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import java.time.LocalDate;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MappingConfig.class)
public interface PoliticalPartyMapper {

    @Mapping(target = "politicalPartyId", source = "politicalParty.id")
    @Mapping(target = "personId", source = "person.id")
    @Mapping(target = "firstName", source = "person.firstName")
    @Mapping(target = "lastName", source = "person.lastName")
    @Mapping(target = "partyName", source = "politicalParty.name")
    @Mapping(target = "partyAbbreviation", source = "politicalParty.abbreviation")
    PartyMembershipDto toPartyMembershipDto(PartyMembership entity);

    PoliticalPartyDto toPoliticalPartyDto(PoliticalParty entity);

    @Mapping(target = "politicalPartyId", source = "politicalParty.id")
    @Mapping(target = "officialId", source = "official.id")
    @Mapping(target = "firstName", source = "official.firstName")
    @Mapping(target = "lastName", source = "official.lastName")
    @Mapping(target = "partyName", source = "politicalParty.name")
    @Mapping(target = "partyAbbreviation", source = "politicalParty.abbreviation")
    PoliticalPartyOfficialDto toPoliticalPartyOfficialDto(PoliticalPartyOfficial entity);

    PoliticalPartyOptionDto toPoliticalPartyOptionDto(PoliticalParty entity);

    @Mapping(target = "politicalPartyId", source = "politicalParty.id")
    PoliticalPartySloganDto toPoliticalPartySloganDto(PoliticalPartySlogan entity);

    @Mapping(target = "politicalPartyId", source = "politicalParty.id")
    PoliticalPartySymbolDto toPoliticalPartySymbolDto(PoliticalPartySymbol entity);

    @Mapping(target = "person", source = "person")
    @Mapping(target = "politicalParty", source = "politicalParty")
    @Mapping(target = "startDate", source = "startDate")
    PartyMembership toMembership(Person person, PoliticalParty politicalParty, LocalDate startDate);

    @Mapping(target = "official", source = "official")
    @Mapping(target = "politicalParty", source = "politicalParty")
    @Mapping(target = "positionName", source = "positionName")
    @Mapping(target = "fromDate", source = "fromDate")
    @Mapping(target = "photo", source = "request.photo")
    @Mapping(target = "about", source = "request.about")
    PoliticalPartyOfficial toOfficial(AppointOfficialRequest request, Person official,
            PoliticalParty politicalParty, String positionName, LocalDate fromDate);

    @Mapping(target = "symbolDescription", expression = "java(request.symbolDescription().trim())")
    @Mapping(target = "politicalParty", source = "politicalParty")
    @Mapping(target = "fromDate", source = "fromDate")
    @Mapping(target = "imageFile", source = "request.imageFile")
    PoliticalPartySymbol toSymbol(AdoptSymbolRequest request, PoliticalParty politicalParty, LocalDate fromDate);

    @Mapping(target = "slogan", expression = "java(request.slogan().trim())")
    @Mapping(target = "politicalParty", source = "politicalParty")
    @Mapping(target = "fromDate", source = "fromDate")
    PoliticalPartySlogan toSlogan(AdoptSloganRequest request, PoliticalParty politicalParty, LocalDate fromDate);

    @Mapping(target = "politicalPartyId", source = "partyId")
    @Mapping(target = "colors", source = "colors")
    PoliticalPartyColorsDto toColorsDto(Long partyId, List<PoliticalPartyColor> colors);

    default String toColorName(PoliticalPartyColor color) {
        return color.getColorName();
    }
}
