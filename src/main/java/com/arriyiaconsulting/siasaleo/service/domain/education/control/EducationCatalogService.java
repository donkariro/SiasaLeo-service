package com.arriyiaconsulting.siasaleo.service.domain.education.control;

import com.arriyiaconsulting.siasaleo.service.domain.education.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.education.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.education.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.OrganizationName;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.OrganizationType;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class EducationCatalogService {
    @Inject private EducationLevelRepository levels;
    @Inject private EducationalInstitutionTypeRepository types;
    @Inject private FieldOfStudyRepository fields;
    @Inject private EducationalInstitutionRepository institutions;
    @PersistenceContext private EntityManager entityManager;

    public List<EducationLevelDto> levels() {
        return levels.findAllOrdered().stream().map(EducationLevelDto::from).toList();
    }

    public List<InstitutionTypeDto> institutionTypes() {
        return types.findAllOrdered().stream().map(InstitutionTypeDto::from).toList();
    }

    public List<FieldOfStudyDto> fields() {
        return fields.findAllOrdered().stream().map(FieldOfStudyDto::from).toList();
    }

    public Optional<InstitutionDto> findInstitution(Long id) {
        return institutions.findById(id).map(InstitutionDto::from);
    }

    public List<InstitutionDto> institutions(Long typeId, String search, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (typeId != null) {
            requireType(typeId);
        }
        String pattern = search == null || search.isBlank() ? null
                : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return institutions.search(typeId, pattern,
                PageRequest.ofPage(page + 1L).size(Math.max(1, Math.min(size, 500))).withoutTotal())
                .stream().map(InstitutionDto::from).toList();
    }

    @Transactional
    public InstitutionDto createInstitution(CreateInstitutionRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.institutionTypeId() == null) {
            throw new IllegalArgumentException("Institution name and type are required");
        }
        EducationalInstitutionType type = requireType(request.institutionTypeId());
        OrganizationType orgType = entityManager.createQuery(
                "SELECT t FROM OrganizationType t WHERE t.typeName = :name", OrganizationType.class)
                .setParameter("name", "EDUCATIONAL_INSTITUTION").getSingleResult();
        return InstitutionDto.from(institutions.save(new EducationalInstitution(
                new OrganizationName(request.name().trim(), null), orgType,
                request.registrationNumber(), type)));
    }

    private EducationalInstitutionType requireType(Long id) {
        return types.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Institution type not found: " + id));
    }
}
