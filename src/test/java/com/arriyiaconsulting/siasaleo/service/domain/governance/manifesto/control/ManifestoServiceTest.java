package com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.control;

import com.arriyiaconsulting.siasaleo.service.domain.candidate.entity.Candidacy;
import com.arriyiaconsulting.siasaleo.service.domain.candidate.repository.CandidacyRepository;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.dto.CreateManifestoRequest;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.Manifesto;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.entity.ManifestoVersion;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.mapping.ManifestoMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository.ManifestoRepository;
import com.arriyiaconsulting.siasaleo.service.domain.governance.manifesto.repository.ManifestoVersionRepository;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.PublicationStatus;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordAccessDeniedException;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import com.arriyiaconsulting.siasaleo.service.security.identity.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManifestoServiceTest {

    @Spy
    private ManifestoMapper mapper = Mappers.getMapper(ManifestoMapper.class);

    @Mock
    private ManifestoRepository manifestos;

    @Mock
    private ManifestoVersionRepository versions;

    @Mock
    private UserAccountRepository accounts;

    @Mock
    private CandidacyRepository candidacies;

    @InjectMocks
    private ManifestoService service;

    @Test
    void anotherPersonsManifestoCannotBePublished() {
        draftVersion(8L, 4L);
        manifestoIssuedBy(4L, 1L);
        account(5L, true, 2L);

        assertThrows(RecordAccessDeniedException.class, () -> service.publish(8L, 5L, false));
        verify(versions, never()).update(any());
    }

    @Test
    void inactiveOwnerCannotPublish() {
        draftVersion(8L, 4L);
        manifestoIssuedBy(4L, 1L);
        UserAccount disabled = mock(UserAccount.class);
        when(disabled.isActive()).thenReturn(false);
        when(accounts.findById(5L)).thenReturn(Optional.of(disabled));

        assertThrows(RecordAccessDeniedException.class, () -> service.publish(8L, 5L, false));
        verify(versions, never()).update(any());
    }

    @Test
    void accountWithoutPersonCannotIssueManifesto() {
        account(5L, true, null);

        assertThrows(RecordAccessDeniedException.class,
                () -> service.create(new CreateManifestoRequest(3L), 5L));
        verify(manifestos, never()).insert(any());
    }

    @Test
    void candidateCannotIssueManifestoForAnotherPersonsCandidacy() {
        account(5L, true, 2L);
        candidacyOf(3L, 1L);

        assertThrows(RecordAccessDeniedException.class,
                () -> service.create(new CreateManifestoRequest(3L), 5L));
        verify(manifestos, never()).insert(any());
    }

    @Test
    void issuerAndCreatorComeFromTheCallingAccount() {
        account(5L, true, 2L);
        candidacyOf(3L, 2L);
        when(manifestos.insert(any())).then(returnsFirstArg());

        service.create(new CreateManifestoRequest(3L), 5L);

        ArgumentCaptor<Manifesto> inserted = ArgumentCaptor.forClass(Manifesto.class);
        verify(manifestos).insert(inserted.capture());
        assertEquals(2L, inserted.getValue().getIssuerPersonId());
        assertEquals(5L, inserted.getValue().getCreatedByAccountId());
        assertEquals(3L, inserted.getValue().getCandidacyId());
    }

    private void draftVersion(long id, long manifestoId) {
        ManifestoVersion version = new ManifestoVersion();
        version.setManifestoId(manifestoId);
        version.setPublicationStatus(PublicationStatus.DRAFT);
        when(versions.findById(id)).thenReturn(Optional.of(version));
    }

    private void manifestoIssuedBy(long id, long personId) {
        Manifesto manifesto = new Manifesto();
        manifesto.setIssuerPersonId(personId);
        when(manifestos.findById(id)).thenReturn(Optional.of(manifesto));
    }

    private void account(long id, boolean active, Long personId) {
        UserAccount account = mock(UserAccount.class);
        when(account.isActive()).thenReturn(active);
        when(account.getPersonId()).thenReturn(personId);
        when(accounts.findById(id)).thenReturn(Optional.of(account));
    }

    private void candidacyOf(long id, long personId) {
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(personId);
        Candidacy candidacy = mock(Candidacy.class);
        when(candidacy.getPerson()).thenReturn(person);
        when(candidacies.findById(id)).thenReturn(Optional.of(candidacy));
    }
}
