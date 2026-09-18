package com.arriyiaconsulting.siasaleo.service.domain.education.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.education.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.education.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.OrganizationName;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.OrganizationType;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MappingConfig.class)
public interface EducationMapper {

    @Mapping(target = "name", source = "levelName")
    EducationLevelDto toEducationLevelDto(EducationLevel entity);

    @Mapping(target = "name", source = "fieldName")
    @Mapping(target = "parentId", source = "parent.id")
    FieldOfStudyDto toFieldOfStudyDto(FieldOfStudy entity);

    @Mapping(target = "institutionTypeId", source = "institutionType.id")
    @Mapping(target = "institutionTypeName", source = "institutionType.typeName")
    InstitutionDto toInstitutionDto(EducationalInstitution entity);

    @Mapping(target = "name", source = "typeName")
    InstitutionTypeDto toInstitutionTypeDto(EducationalInstitutionType entity);

    @Mapping(target = "personId", source = "person.id")
    @Mapping(target = "educationLevelId", source = "educationLevel.id")
    @Mapping(target = "educationLevelName", source = "educationLevel.levelName")
    @Mapping(target = "institutionId", source = "institution.id")
    @Mapping(target = "institutionName", source = "institution.name")
    @Mapping(target = "fieldOfStudyId", source = "fieldOfStudy.id")
    @Mapping(target = "fieldOfStudyName", source = "fieldOfStudy.fieldName")
    PersonEducationDto toPersonEducationDto(PersonEducation entity);

    @Mapping(target = "person", source = "person")
    @Mapping(target = "level", source = "level")
    @Mapping(target = "institution", source = "institution")
    @Mapping(target = "field", source = "field")
    @Mapping(target = "fromDate", source = "request.fromDate")
    @Mapping(target = "uptoDate", source = "request.uptoDate")
    PersonEducation toEntity(EducationRequest request, Person person,
            EducationLevel level, EducationalInstitution institution, FieldOfStudy field);

    @Mapping(target = "name", expression = "java(new OrganizationName(request.name().trim(), null))")
    @Mapping(target = "orgType", source = "orgType")
    @Mapping(target = "institutionType", source = "institutionType")
    @Mapping(target = "registrationNumber", source = "request.registrationNumber")
    EducationalInstitution toEntity(CreateInstitutionRequest request, OrganizationType orgType,
            EducationalInstitutionType institutionType);
}
