package com.arriyiaconsulting.siasaleo.service.domain.nomination.control;

import com.arriyiaconsulting.siasaleo.service.domain.forms.control.FormInstanceService;
import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.FormSubmissionDto;
import com.arriyiaconsulting.siasaleo.service.domain.nomination.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.nomination.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.nomination.repository.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.json.JsonObject;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NominationServiceTest {
    @Mock private NominationExerciseRepository exercises;
    @Mock private NominationApplicationRepository applications;
    @Mock private NominationRecords records;
    @Mock private FormInstanceService forms;
    @InjectMocks private NominationService service;

    private static final long PARTY = 3, MANAGER = 10, APPLICANT = 20, PERSON = 200, CONTEST = 50;

    @Test void onlyAssignedManagersCreateExercises() {
        when(records.partyExists(PARTY)).thenReturn(true);
        var request = new CreateNominationExerciseRequest(PARTY, 1L, "Primaries", now().minusDays(1), now().plusDays(1));
        assertThrows(RecordAccessDeniedException.class, () -> service.create(request, APPLICANT));
        verify(exercises, never()).insert(any());
    }

    @Test void anotherPartysFormCannotBeBound() {
        exercise(ExerciseStatus.DRAFT); manager();
        when(forms.published(70)).thenReturn(new FormInstanceService.PublishedForm(70, NominationService.FORM_PURPOSE, 99L));
        when(forms.tenant(NominationService.TENANT_TYPE, "3")).thenReturn(8L);
        assertThrows(IllegalArgumentException.class, () -> service.bindForm(1, NominationFormPurpose.INITIAL_APPLICATION, 70, MANAGER));
        verify(records, never()).bindForm(anyLong(), any(), anyLong());
    }

    @Test void platformCoreOrOwnExtensionCanBeBound() {
        exercise(ExerciseStatus.DRAFT); manager();
        when(forms.published(70)).thenReturn(new FormInstanceService.PublishedForm(70, NominationService.FORM_PURPOSE, null));
        service.bindForm(1, NominationFormPurpose.INITIAL_APPLICATION, 70, MANAGER);
        verify(records).bindForm(1, NominationFormPurpose.INITIAL_APPLICATION, 70);
    }

    @Test void exerciseOpensOnlyWithAnApplicationForm() {
        NominationExercise exercise = exercise(ExerciseStatus.DRAFT); manager();
        when(records.exerciseForms(1)).thenReturn(Map.of());
        assertThrows(IllegalArgumentException.class, () -> service.open(1, exercise.getRowVersion(), MANAGER));
    }

    @Test void nonMembersCannotApply() {
        exercise(ExerciseStatus.OPEN);
        when(records.personOf(APPLICANT)).thenReturn(Optional.of(PERSON));
        when(records.membershipStart(PERSON, PARTY)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.apply(1, CONTEST, APPLICANT));
        verify(applications, never()).insert(any());
    }

    @Test void applyingPinsTheExerciseFormForThePartyTenant() {
        exercise(ExerciseStatus.OPEN);
        when(records.personOf(APPLICANT)).thenReturn(Optional.of(PERSON));
        when(records.membershipStart(PERSON, PARTY)).thenReturn(Optional.of("2024-01-01"));
        when(records.contestInEvent(CONTEST, 4)).thenReturn(true);
        when(records.exerciseForms(1)).thenReturn(Map.of(NominationFormPurpose.INITIAL_APPLICATION, 70L));
        when(applications.insert(any())).then(call -> set(call.getArgument(0), "id", 77L));
        when(forms.tenant(NominationService.TENANT_TYPE, "3")).thenReturn(8L);
        when(forms.open(70, 8L, APPLICANT)).thenReturn(600L);
        when(records.applicationForm(77L, NominationFormPurpose.INITIAL_APPLICATION)).thenReturn(Optional.of(600L));
        service.apply(1, CONTEST, APPLICANT);
        verify(records).linkForm(77L, NominationFormPurpose.INITIAL_APPLICATION, 600L);
    }

    @Test void closedExerciseRejectsEdits() {
        exercise(ExerciseStatus.CLOSED); application();
        when(records.personOf(APPLICANT)).thenReturn(Optional.of(PERSON));
        assertThrows(IllegalArgumentException.class,
                () -> service.save(7, new SaveApplicationAnswersRequest(JsonObject.EMPTY_JSON_OBJECT, 0L), APPLICANT));
        verifyNoInteractions(forms);
    }

    @Test void othersCannotSeeAnApplication() {
        exercise(ExerciseStatus.OPEN); application();
        when(records.personOf(MANAGER)).thenReturn(Optional.of(999L));
        assertThrows(RecordNotFoundException.class, () -> service.application(7, MANAGER));
    }

    @Test void submissionSnapshotsContextAndMarksApplicationSubmitted() {
        exercise(ExerciseStatus.OPEN); NominationApplication application = application();
        when(records.personOf(APPLICANT)).thenReturn(Optional.of(PERSON));
        when(records.membershipStart(PERSON, PARTY)).thenReturn(Optional.of("2024-01-01"));
        when(records.applicationForm(7L, NominationFormPurpose.INITIAL_APPLICATION)).thenReturn(Optional.of(600L));
        OffsetDateTime at = now();
        when(forms.submit(eq(600L), eq(4L), eq(Set.of("core.truth")), any(), eq(APPLICANT)))
                .thenReturn(new FormSubmissionDto(90L, 1, 70L, "c", null, null, Map.of(), Map.of(), APPLICANT, at));
        service.submit(7, new SubmitApplicationRequest(4L, List.of("core.truth")), APPLICANT);
        ArgumentCaptor<JsonObject> context = ArgumentCaptor.forClass(JsonObject.class);
        verify(forms).submit(eq(600L), eq(4L), any(), context.capture(), eq(APPLICANT));
        assertEquals("2024-01-01", context.getValue().getString("membership_start_date"));
        assertEquals(CONTEST, context.getValue().getJsonNumber("contest_id").longValue());
        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
        assertEquals(at, application.getSubmittedAt());
    }

    /** Stands in for the identity and version JPA assigns on insert. */
    private static <T> T set(T entity, String field, Object value) {
        try {
            var f = entity.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(entity, value);
            return entity;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static OffsetDateTime now() { return OffsetDateTime.now(); }
    private void manager() { when(records.manager(PARTY, MANAGER)).thenReturn(true); }
    private NominationExercise exercise(ExerciseStatus status) {
        NominationExercise exercise = new NominationExercise(PARTY, 4L, "Primaries", now().minusDays(1), now().plusDays(1), MANAGER);
        if (status != ExerciseStatus.DRAFT) exercise.open();
        if (status == ExerciseStatus.CLOSED) exercise.close();
        set(exercise, "id", 1L); set(exercise, "rowVersion", 0L);
        lenient().when(exercises.findById(1L)).thenReturn(Optional.of(exercise));
        return exercise;
    }
    private NominationApplication application() {
        NominationApplication application = set(new NominationApplication(1L, PERSON, CONTEST, APPLICANT), "id", 7L);
        when(applications.findById(7L)).thenReturn(Optional.of(application));
        return application;
    }
}
