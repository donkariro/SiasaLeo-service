package com.arriyiaconsulting.siasaleo.service.domain.education.control;

import com.arriyiaconsulting.siasaleo.service.domain.education.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.education.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.education.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.party.control.PersonProfileService;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EducationServiceTest {
    @Mock PersonEducationRepository education;
    @Mock EducationLevelRepository levels;
    @Mock EducationalInstitutionRepository institutions;
    @Mock FieldOfStudyRepository fields;
    @Mock PersonRepository persons;
    @Mock PersonProfileService profiles;
    @InjectMocks EducationService service;

    private Person person(long id) {
        Person person = mock(Person.class);
        lenient().when(person.getId()).thenReturn(id);
        return person;
    }

    private EducationLevel level() {
        EducationLevel level = mock(EducationLevel.class);
        lenient().when(level.getId()).thenReturn(5L);
        lenient().when(level.getLevelName()).thenReturn("BACHELORS_DEGREE");
        return level;
    }

    private EducationRequest minimal() {
        return new EducationRequest(5L, null, null, null, null);
    }

    @Test
    void createsOwnRecordWithOptionalFieldsAbsent() {
        Person person = person(7);
        when(profiles.findFor(10L)).thenReturn(Optional.of(person));
        when(persons.findById(7L)).thenReturn(Optional.of(person));
        doReturn(Optional.of(level())).when(levels).findById(5L);
        when(education.save(any(PersonEducation.class))).thenAnswer(call -> call.getArgument(0));

        PersonEducationDto result = service.addFor(10L, minimal());

        assertEquals(7L, result.personId());
        assertEquals(5L, result.educationLevelId());
        assertNull(result.institutionId());
        assertNull(result.fieldOfStudyId());
        assertNull(result.fromDate());
        assertNull(result.uptoDate());
    }

    @Test
    void mapsFullQualificationAndAllowsSameDayStudy() {
        Person person = person(7);
        EducationalInstitution institution = mock(EducationalInstitution.class);
        when(institution.getId()).thenReturn(9L);
        when(institution.getName()).thenReturn("University");
        FieldOfStudy field = mock(FieldOfStudy.class);
        when(field.getId()).thenReturn(11L);
        when(field.getFieldName()).thenReturn("LAW");
        when(persons.findById(7L)).thenReturn(Optional.of(person));
        doReturn(Optional.of(level())).when(levels).findById(5L);
        when(institutions.findById(9L)).thenReturn(Optional.of(institution));
        when(fields.findById(11L)).thenReturn(Optional.of(field));
        when(education.save(any(PersonEducation.class))).thenAnswer(call -> call.getArgument(0));
        LocalDate day = LocalDate.of(2020, 1, 1);

        PersonEducationDto result = service.add(7L, new EducationRequest(5L, 9L, 11L, day, day));

        assertEquals("University", result.institutionName());
        assertEquals("LAW", result.fieldOfStudyName());
        assertEquals(day, result.uptoDate());
    }

    @Test
    void rejectsReversedDatesWithoutSaving() {
        doReturn(Optional.of(person(7))).when(persons).findById(7L);
        assertThrows(IllegalArgumentException.class, () -> service.add(7L,
                new EducationRequest(5L, null, null, LocalDate.of(2021, 1, 1), LocalDate.of(2020, 1, 1))));
        verifyNoInteractions(education, levels);
    }

    @ParameterizedTest
    @ValueSource(strings = {"level", "institution", "field"})
    void rejectsUnknownReferences(String missing) {
        doReturn(Optional.of(person(7))).when(persons).findById(7L);
        if (!missing.equals("level")) {
            doReturn(Optional.of(level())).when(levels).findById(5L);
        }
        EducationRequest request = new EducationRequest(5L,
                missing.equals("institution") ? 9L : null, missing.equals("field") ? 11L : null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.add(7L, request));
        verify(education, never()).save(any(PersonEducation.class));
    }

    @Test
    void refusesWritesWithoutLinkedProfile() {
        assertThrows(IllegalArgumentException.class, () -> service.addFor(10L, minimal()));
        verifyNoInteractions(education);
    }

    @Test
    void unlinkedProfileHasEmptyHistory() {
        assertEquals(List.of(), service.findFor(10L));
    }

    @Test
    void unknownPersonIsNotAnEmptyHistory() {
        assertThrows(NoSuchElementException.class, () -> service.findByPerson(999L));
    }

    @Test
    void anotherPersonsRecordCannotBeReadUpdatedOrDeleted() {
        PersonEducation record = new PersonEducation(person(8), level(), null, null, null, null);
        doReturn(Optional.of(person(7))).when(profiles).findFor(10L);
        when(education.findById(3L)).thenReturn(Optional.of(record));

        assertThrows(NoSuchElementException.class, () -> service.findByIdFor(10L, 3L));
        assertThrows(NoSuchElementException.class, () -> service.updateFor(10L, 3L, minimal()));
        assertThrows(NoSuchElementException.class, () -> service.deleteFor(10L, 3L));
        verify(education, never()).save(any(PersonEducation.class));
        verify(education, never()).delete(any(PersonEducation.class));
    }

    @Test
    void updateCanClearOptionalReferencesAndDates() {
        PersonEducation record = new PersonEducation(person(7), level(),
                mock(EducationalInstitution.class), mock(FieldOfStudy.class),
                LocalDate.of(2020, 1, 1), LocalDate.of(2024, 1, 1));
        when(education.findById(3L)).thenReturn(Optional.of(record));
        doReturn(Optional.of(level())).when(levels).findById(5L);
        when(education.save(record)).thenReturn(record);

        PersonEducationDto result = service.update(7L, 3L, minimal());

        assertNull(result.institutionId());
        assertNull(result.fieldOfStudyId());
        assertNull(result.fromDate());
        assertNull(result.uptoDate());
        assertEquals(7L, result.personId());
    }

    @Test
    void deletesOwnedRecord() {
        PersonEducation record = new PersonEducation(person(7), level(), null, null, null, null);
        when(education.findById(3L)).thenReturn(Optional.of(record));
        service.delete(7L, 3L);
        verify(education).delete(record);
    }

    @Test
    void missingRecordIsNotFound() {
        assertThrows(NoSuchElementException.class, () -> service.update(7L, 3L, minimal()));
        assertThrows(NoSuchElementException.class, () -> service.delete(7L, 3L));
    }

    @Test
    void entityRejectsInvalidDatesBeforeChangingExistingRecord() {
        LocalDate start = LocalDate.of(2020, 1, 1);
        PersonEducation record = new PersonEducation(person(7), level(), null, null, start, null);
        assertThrows(IllegalArgumentException.class,
                () -> record.revise(level(), null, null, start, start.minusDays(1)));
        assertEquals(start, record.getFromDate());
        assertNull(record.getUptoDate());
    }
}
