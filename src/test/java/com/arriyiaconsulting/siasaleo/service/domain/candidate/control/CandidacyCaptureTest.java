package com.arriyiaconsulting.siasaleo.service.domain.candidate.control;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.mapping.CandidacyMapper;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.entity.Contest;
import com.arriyiaconsulting.siasaleo.service.domain.election.repository.ContestRepository;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.domain.party.control.PersonProfileService;
import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegistrationService;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidacyCaptureTest {
    @Mock CandidacyRepository candidacies;
    @Mock CandidacyStatusRepository statuses;
    @Mock ContestRepository contests;
    @Mock PersonRepository persons;
    @Mock VoterRegistrationService voters;
    @Mock PersonProfileService profiles;
    @Spy CandidacyMapper candidacyMapper=Mappers.getMapper(CandidacyMapper.class);
    @InjectMocks CandidacyService service;
    private ImportCandidacyRequest request() { return new ImportCandidacyRequest(1L,2L,null,null,9L,"Ballot A",null,"gazette","row1"); }
    @Test void administrativeCaptureUsesCoreCandidacyWithoutAccountOrVoterDeclaration() {
        Contest contest=mock(Contest.class); when(contest.getId()).thenReturn(1L);
        when(contests.findById(1L)).thenReturn(Optional.of(contest));
        Person person=mock(Person.class);when(person.getId()).thenReturn(2L);when(person.getFirstName()).thenReturn("Profile");when(person.getLastName()).thenReturn("Name");
        when(persons.findById(2L)).thenReturn(Optional.of(person));
        when(statuses.findById(9L)).thenReturn(Optional.of(mock(CandidacyStatus.class)));
        when(candidacies.save(any())).thenAnswer(i->i.getArgument(0));
        CandidacyDto result=service.importCandidacy(request());
        assertEquals(1L,result.contestId());assertEquals("Ballot A",result.ballotName());
        verifyNoInteractions(voters,profiles);
        verify(candidacies).save(any(Candidacy.class));
    }
    @Test void exactSourceReplayDoesNotCreateAnotherPersonOrCandidacy() {
        Candidacy existing=mock(Candidacy.class);
        when(existing.getImportFingerprint()).thenReturn(ImportSupport.fingerprint(request()));
        when(candidacies.findBySource(1L,"gazette","row1")).thenReturn(Optional.of(existing));
        service.importCandidacy(request());
        verifyNoInteractions(persons,contests,voters,profiles);
        verify(candidacies,never()).save(any());
    }
    @Test void ballotNameDoesNotFollowProfileEdits() {
        Person person=new Person("Original","Name",null,null);
        Candidacy c=new Candidacy(person,new Contest(1L,2L,null),null,null);
        person.updateDetails("New","Name",null,null);
        assertEquals("Original Name",c.getBallotName());
    }
}
