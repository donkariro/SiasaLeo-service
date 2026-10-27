package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control.DigitalAssetService;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.forms.repository.FormStore;
import com.arriyiaconsulting.siasaleo.service.domain.forms.repository.FormStore.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.arriyiaconsulting.siasaleo.service.domain.forms.control.FormSamples.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormInstanceServiceTest {
    @Mock private FormStore store;
    @Mock private DigitalAssetService assets;
    @Spy private FormValidator validator = new FormValidator();
    @InjectMocks private FormInstanceService service;

    private static final Set<String> ALL = Set.of("core.truth", "ext.code");
    private static final JsonObject COMPLETE = json("""
        {'core':{'phone':'0700','publicOfficer':true,'resignation':41},'ext':{'disciplinary':false}}
        """);

    @Test void repeatedSubmitReturnsTheExistingRevision() {
        instance(COMPLETE, 3);
        when(store.submittedAt(5, 3)).thenReturn(Optional.of(submission()));
        assertEquals(1, service.submit(5, 3, ALL, null, 7).revision());
        verify(store, never()).insertSubmission(any(), any(), any(), any(), anyLong());
        verifyNoInteractions(assets);
    }

    @Test void staleDraftIsAConflict() {
        instance(COMPLETE, 4);
        assertThrows(RecordConflictException.class, () -> service.submit(5, 3, ALL, null, 7));
    }

    @Test void documentsMustBeSealedAndReadableBySubmitter() {
        instance(COMPLETE, 3); version();
        when(assets.read(41, 7L)).thenThrow(new RecordNotFoundException());
        InvalidFormException e = assertThrows(InvalidFormException.class, () -> service.submit(5, 3, ALL, null, 7));
        assertEquals("core.resignation", e.getProblems().getFirst().field());
        verify(store, never()).insertSubmission(any(), any(), any(), any(), anyLong());
    }

    @Test void submissionRecordsEvidenceAndDeclarationWording() {
        instance(COMPLETE, 3); version();
        when(assets.read(41, 7L)).thenReturn(sealed());
        when(store.insertSubmission(any(), any(), any(), any(), eq(7L))).thenReturn(90L);
        when(store.submission(90)).thenReturn(Optional.of(submission()));
        service.submit(5, 3, ALL, JsonValue.EMPTY_JSON_OBJECT, 7);
        verify(store).insertEvidence(90, "core.resignation", 41);
        verify(store).insertDeclaration(90, "core.truth", "The information I have given is true.");
        verify(store).insertDeclaration(90, "ext.code", "I accept the party code of conduct.");
    }

    @Test void draftsWithWrongTypesAreNotSaved() {
        instance(JsonValue.EMPTY_JSON_OBJECT, 3); version();
        assertThrows(InvalidFormException.class, () -> service.save(5, 3, json("{'core':{'publicOfficer':'yes'}}")));
        verify(store, never()).saveAnswers(anyLong(), anyLong(), any());
    }

    private void instance(JsonObject answers, long rowVersion) {
        when(store.lockInstance(5)).thenReturn(Optional.of(new Instance(5, 2, 1L, answers, rowVersion, OffsetDateTime.now())));
    }
    private void version() {
        when(store.version(2)).thenReturn(Optional.of(new Version(2, 1, 1, 9L, "PUBLISHED", JsonValue.EMPTY_JSON_OBJECT, party(),
                "c".repeat(64), 1, OffsetDateTime.now(), OffsetDateTime.now())));
    }
    private static Submission submission() {
        return new Submission(90, 5, 1, 2, "c".repeat(64), COMPLETE, JsonValue.EMPTY_JSON_OBJECT, 7, OffsetDateTime.now());
    }
    private static DigitalAssetDetailDto sealed() {
        var version = new DigitalAssetVersionDto(41L, 4L, 1, null, null, null, null, null, 7L, OffsetDateTime.now(),
                OffsetDateTime.now(), 1L, "Letter", null, "DOCUMENT");
        return new DigitalAssetDetailDto(null, version, List.of());
    }
}
