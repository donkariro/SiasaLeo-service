package com.arriyiaconsulting.siasaleo.service.domain.voter.control;

import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegisterDtos.*;
import com.arriyiaconsulting.siasaleo.service.domain.voter.repository.VoterRegisterRepository;
import java.util.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.Coverage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.validation.TestValidation;
import jakarta.validation.Validator;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.PublicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoterRegisterServiceTest {
    @Mock VoterRegisterRepository repo;
    @Spy Validator validator=TestValidation.validator();
    @InjectMocks VoterRegisterService service;
    private RegisterDto publication(PublicationStatus status) {
        return new RegisterDto(1L,1L,2L,7L,"source","a".repeat(64),Coverage.COMPLETE,null,status,"today",null);
    }
    @Test void hidesDraftFromPublicCaller() {
        when(repo.find(1,false)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        assertThrows(RecordNotFoundException.class,()->service.find(1,false));
        assertEquals(PublicationStatus.DRAFT,service.find(1,true).status());
    }
    @Test void incompletePublicationIsNotPublished() {
        when(repo.find(1,true)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        when(repo.find(1,false)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        when(repo.errors(1)).thenReturn(List.of("Missing station"));
        assertThrows(IllegalArgumentException.class,()->service.publish(1));
        verify(repo,never()).execute(anyString(),any(Object[].class));
    }
    @Test void repeatPublicationDoesNotRewritePublishedRecord() {
        when(repo.find(1,true)).thenReturn(Optional.of(publication(PublicationStatus.PUBLISHED)));
        assertEquals(PublicationStatus.PUBLISHED,service.publish(1).status());
        verify(repo,never()).execute(anyString(),any(Object[].class));
    }
    @Test void cannotDeletePublishedData() {
        when(repo.find(1,true)).thenReturn(Optional.of(publication(PublicationStatus.PUBLISHED)));
        assertThrows(RecordConflictException.class,()->service.deleteDraft(1));
        verify(repo,never()).execute(anyString(),any(Object[].class));
    }
    @Test void errorsInPreviewPreventAnyMutation() {
        assertThrows(IllegalArgumentException.class,()->service.create(null));
        verifyNoInteractions(repo);
    }
    private RegisterRequest request() {
        return new RegisterRequest(1L,2L,7L,"source","a".repeat(64),"COMPLETE",null,
                List.of(new RegisterCount(9L,100L,null)));
    }
    @Test void exactSourceReplayReturnsExistingPublicationWithoutInserting() {
        RegisterRequest r=request();
        when(repo.exists(anyString(),any(Object[].class))).thenReturn(true);
        when(repo.rows(startsWith("SELECT id,import_fingerprint"),any(Object[].class)))
                .thenReturn(Collections.singletonList(new Object[]{1L,ImportSupport.fingerprint(r)}));
        when(repo.find(1,false)).thenReturn(Optional.of(publication(PublicationStatus.PUBLISHED)));
        when(repo.errors(1)).thenReturn(List.of());
        RegisterImportResult result=service.create(r);
        assertTrue(result.replayed());
        assertEquals(1L,result.register().id());
        verify(repo,never()).insert(anyString(),any(Object[].class));
        verify(repo,never()).execute(anyString(),any(Object[].class));
    }
    @Test void changedPayloadCannotSilentlyOverwriteSameSource() {
        RegisterRequest r=request();
        when(repo.exists(anyString(),any(Object[].class))).thenReturn(true);
        when(repo.rows(startsWith("SELECT id,import_fingerprint"),any(Object[].class)))
                .thenReturn(Collections.singletonList(new Object[]{1L,"different fingerprint"}));
        assertThrows(RecordConflictException.class,()->service.create(r));
        verify(repo,never()).insert(anyString(),any(Object[].class));
    }
    // Stored coverage is an enum while the request carries text; a batch whose
    // metadata matches must be recognised as matching, not rejected.
    @Test void batchWithMatchingCoverageIsAcceptedAndDifferentCoverageRejected() {
        when(repo.find(1,true)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        when(repo.find(1,false)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        when(repo.exists(anyString(),any(Object[].class))).thenReturn(true);
        assertTrue(service.append(1,request()).replayed());
        RegisterRequest partial=new RegisterRequest(1L,2L,7L,"source","a".repeat(64),"PARTIAL",null,request().counts());
        assertThrows(IllegalArgumentException.class,()->service.append(1,partial));
    }
}
