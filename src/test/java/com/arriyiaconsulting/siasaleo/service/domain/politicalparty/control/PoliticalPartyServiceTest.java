package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartyDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import jakarta.data.page.PageRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the read side of the party register: the DTO must carry the
 * name that only became reachable once organization_name (V17) was mapped, the
 * fields the ORPP register leaves blank must survive as nulls, and the 0-based
 * REST page must be translated to Jakarta Data's 1-based one.
 */
@ExtendWith(MockitoExtension.class)
class PoliticalPartyServiceTest {

    @Mock
    private PoliticalPartyRepository politicalParties;

    @InjectMocks
    private PoliticalPartyService service;

    @Test
    void findByIdMapsTheWholeRegisterEntry() {
        LocalDate registeredOn = LocalDate.of(2012, 3, 19);
        PoliticalParty party = mock(PoliticalParty.class);
        when(party.getId()).thenReturn(3L);
        when(party.getName()).thenReturn("People's Liberation Party");
        when(party.getAbbreviation()).thenReturn("PLP");
        when(party.getRegistrationNumber()).thenReturn("001");
        when(party.getRegisteredOn()).thenReturn(registeredOn);
        when(party.getPostalAddress()).thenReturn("34200-00100 Nairobi");
        when(party.getHeadOfficeLocation()).thenReturn("NARC-Kenya House, Woodland Road");
        when(party.getChanges()).thenReturn("Formerly NARC-KENYA");
        when(politicalParties.findById(3L)).thenReturn(Optional.of(party));

        PoliticalPartyDto found = service.findById(3L).orElseThrow();

        assertEquals(3L, found.id());
        assertEquals("People's Liberation Party", found.name());
        assertEquals("PLP", found.abbreviation());
        assertEquals("001", found.registrationNumber());
        assertEquals(registeredOn, found.registeredOn());
        assertEquals("34200-00100 Nairobi", found.postalAddress());
        assertEquals("NARC-Kenya House, Woodland Road", found.headOfficeLocation());
        assertEquals("Formerly NARC-KENYA", found.changes());
    }

    // The register leaves everything but the name blank for some parties.
    @Test
    void findByIdKeepsUnrecordedRegisterFieldsNull() {
        PoliticalParty party = givenParty(4L, "Msingi wa Utaifa");
        when(politicalParties.findById(4L)).thenReturn(Optional.of(party));

        PoliticalPartyDto found = service.findById(4L).orElseThrow();

        assertEquals("Msingi wa Utaifa", found.name());
        assertNull(found.abbreviation());
        assertNull(found.registrationNumber());
        assertNull(found.registeredOn());
        assertNull(found.postalAddress());
        assertNull(found.headOfficeLocation());
        assertNull(found.changes());
    }

    @Test
    void findByIdIsEmptyForAnUnknownParty() {
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        assertTrue(service.findById(999L).isEmpty());
    }

    @Test
    void listPreservesTheNameOrderTheQueryImposes() {
        // Built before the outer when(): Mockito rejects stubbing nested
        // inside another stubbing call.
        List<PoliticalParty> register = List.of(givenParty(1L, "Amani National Congress"),
                givenParty(2L, "Jubilee Party"));
        when(politicalParties.findAllOrderedByName(any(PageRequest.class)))
                .thenReturn(register);

        List<PoliticalPartyDto> found = service.findAll(0, 100);

        assertEquals(List.of("Amani National Congress", "Jubilee Party"),
                found.stream().map(PoliticalPartyDto::name).toList());
    }

    @Test
    void listTranslatesTheZeroBasedPageAndSkipsTheCount() {
        when(politicalParties.findAllOrderedByName(any(PageRequest.class)))
                .thenReturn(List.of());

        service.findAll(2, 25);

        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(politicalParties).findAllOrderedByName(pageRequest.capture());
        // REST layer is 0-based, Jakarta Data is 1-based.
        assertEquals(3L, pageRequest.getValue().page());
        assertEquals(25, pageRequest.getValue().size());
        // The register is listed, never counted; requesting a total would cost
        // a second query for a page the client already gets whole.
        assertFalse(pageRequest.getValue().requestTotal());
    }

    private static PoliticalParty givenParty(long id, String name) {
        PoliticalParty party = mock(PoliticalParty.class);
        lenient().when(party.getId()).thenReturn(id);
        lenient().when(party.getName()).thenReturn(name);
        return party;
    }
}
