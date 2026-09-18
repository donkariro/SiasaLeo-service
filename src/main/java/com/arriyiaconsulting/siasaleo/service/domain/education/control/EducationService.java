package com.arriyiaconsulting.siasaleo.service.domain.education.control;

import com.arriyiaconsulting.siasaleo.service.domain.education.mapping.EducationMapper;
import com.arriyiaconsulting.siasaleo.service.domain.education.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.education.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.education.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.party.control.PersonProfileService;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class EducationService {

    @Inject
    private EducationMapper educationMapper;
    @Inject private PersonEducationRepository education;
    @Inject private EducationLevelRepository levels;
    @Inject private EducationalInstitutionRepository institutions;
    @Inject private FieldOfStudyRepository fields;
    @Inject private PersonRepository persons;
    @Inject private PersonProfileService profiles;

    public List<PersonEducationDto> findByPerson(Long personId) {
        requirePerson(personId);
        return education.findByPerson(personId).stream().map(educationMapper::toPersonEducationDto).toList();
    }

    public List<PersonEducationDto> findFor(Long accountId) {
        return profiles.findFor(accountId)
                .map(person -> findByPerson(person.getId())).orElseGet(List::of);
    }

    @Transactional
    public PersonEducationDto addFor(Long accountId, EducationRequest request) {
        return add(requireOwnPerson(accountId).getId(), request);
    }

    @Transactional
    public PersonEducationDto updateFor(Long accountId, Long id, EducationRequest request) {
        return update(requireOwnPerson(accountId).getId(), id, request);
    }

    @Transactional
    public void deleteFor(Long accountId, Long id) {
        delete(requireOwnPerson(accountId).getId(), id);
    }

    @Transactional
    public PersonEducationDto add(Long personId, EducationRequest request) {
        Person person = requirePerson(personId);
        Resolved resolved = resolve(request);
        return educationMapper.toPersonEducationDto(education.save(educationMapper.toEntity(request, person, resolved.level(),
                resolved.institution(), resolved.field())));
    }

    @Transactional
    public PersonEducationDto update(Long personId, Long id, EducationRequest request) {
        PersonEducation existing = requireOwned(personId, id);
        Resolved resolved = resolve(request);
        existing.revise(resolved.level(), resolved.institution(), resolved.field(),
                request.fromDate(), request.uptoDate());
        return educationMapper.toPersonEducationDto(education.save(existing));
    }

    @Transactional
    public void delete(Long personId, Long id) {
        education.delete(requireOwned(personId, id));
    }

    public PersonEducationDto findById(Long personId, Long id) {
        return educationMapper.toPersonEducationDto(requireOwned(personId, id));
    }

    public PersonEducationDto findByIdFor(Long accountId, Long id) {
        return findById(requireOwnPerson(accountId).getId(), id);
    }

    private PersonEducation requireOwned(Long personId, Long id) {
        return education.findById(id)
                .filter(record -> record.getPerson().getId().equals(personId))
                .orElseThrow(() -> new NoSuchElementException("Education record not found: " + id));
    }

    private Person requirePerson(Long personId) {
        return persons.findById(personId)
                .orElseThrow(() -> new NoSuchElementException("Person not found: " + personId));
    }

    private Person requireOwnPerson(Long accountId) {
        return profiles.findFor(accountId).orElseThrow(() ->
                new IllegalArgumentException("A linked person profile is required to manage education"));
    }

    private Resolved resolve(EducationRequest request) {
        if (request == null || request.educationLevelId() == null) {
            throw new IllegalArgumentException("Education level is required");
        }
        if (request.fromDate() != null && request.uptoDate() != null
                && request.uptoDate().isBefore(request.fromDate())) {
            throw new IllegalArgumentException("Study end date must not precede start date");
        }
        EducationLevel level = levels.findById(request.educationLevelId())
                .orElseThrow(() -> new IllegalArgumentException("Education level not found"));
        EducationalInstitution institution = request.institutionId() == null ? null
                : institutions.findById(request.institutionId())
                        .orElseThrow(() -> new IllegalArgumentException("Educational institution not found"));
        FieldOfStudy field = request.fieldOfStudyId() == null ? null
                : fields.findById(request.fieldOfStudyId())
                        .orElseThrow(() -> new IllegalArgumentException("Field of study not found"));
        return new Resolved(level, institution, field);
    }

    private record Resolved(EducationLevel level, EducationalInstitution institution, FieldOfStudy field) {}
}
