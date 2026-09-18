package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import org.mapstruct.factory.Mappers;
import org.mockito.Spy;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.mapping.PoliticalPartyMapper;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.AdoptSymbolRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartySymbolDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartySymbol;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartySymbolRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the invariant idx_politicalparty_symbol_current enforces: a
 * party shows exactly one symbol at a time. Adopting one must close the symbol
 * in use the day before — ranges being inclusive of both dates — and must get
 * that closure to the database before the new open row is inserted.
 */
@ExtendWith(MockitoExtension.class)
class PoliticalPartySymbolServiceTest {

    @Spy
    private PoliticalPartyMapper politicalPartyMapper = Mappers.getMapper(PoliticalPartyMapper.class);


    private static final long PARTY_ID = 3L;

    @Mock
    private PoliticalPartySymbolRepository symbols;

    @Mock
    private PoliticalPartyRepository politicalParties;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PoliticalPartySymbolService service;

    @Test
    void adoptRecordsTheSymbolWhenThePartyHasNone() {
        givenParty();
        givenNoCurrentSymbol();
        savePassesThrough();

        PoliticalPartySymbolDto adopted = service.adopt(PARTY_ID,
                new AdoptSymbolRequest("Key", "party_007_009_KNC.jpeg", null));

        assertEquals(PARTY_ID, adopted.politicalPartyId());
        assertEquals("Key", adopted.symbolDescription());
        assertEquals("party_007_009_KNC.jpeg", adopted.imageFile());
        assertEquals(LocalDate.now(), adopted.fromDate());
        assertNull(adopted.uptoDate());
        assertTrue(adopted.current());
        verify(entityManager, never()).flush();
    }

    @Test
    void adoptKeepsABackfilledStartDate() {
        givenParty();
        givenNoCurrentSymbol();
        savePassesThrough();

        LocalDate cameIn = LocalDate.of(2012, 4, 16);
        PoliticalPartySymbolDto adopted = service.adopt(PARTY_ID,
                new AdoptSymbolRequest("Key", null, cameIn));

        assertEquals(cameIn, adopted.fromDate());
    }

    @Test
    void adoptTrimsTheDescription() {
        givenParty();
        givenNoCurrentSymbol();
        savePassesThrough();

        PoliticalPartySymbolDto adopted = service.adopt(PARTY_ID,
                new AdoptSymbolRequest("  Key  ", null, null));

        assertEquals("Key", adopted.symbolDescription());
    }

    @Test
    void adoptClosesTheSymbolInUseTheDayBeforeTheNewOneComesIn() {
        givenParty();
        PoliticalPartySymbol inUse = givenCurrentSymbol("Key", LocalDate.of(2012, 4, 16));
        savePassesThrough();

        LocalDate cameIn = LocalDate.of(2022, 1, 10);
        PoliticalPartySymbolDto adopted = service.adopt(PARTY_ID,
                new AdoptSymbolRequest("Crown", null, cameIn));

        // Inclusive ranges: closing on the day the successor starts would show
        // the party two symbols that day.
        assertEquals(cameIn.minusDays(1), inUse.getUptoDate());
        assertFalse(inUse.isCurrent());
        assertEquals("Crown", adopted.symbolDescription());
        assertTrue(adopted.current());
    }

    @Test
    void adoptFlushesTheClosureBeforeInsertingTheNewSymbol() {
        givenParty();
        PoliticalPartySymbol inUse = givenCurrentSymbol("Key", LocalDate.of(2012, 4, 16));
        savePassesThrough();

        service.adopt(PARTY_ID, new AdoptSymbolRequest("Crown", null,
                LocalDate.of(2022, 1, 10)));

        // The update closing the old row has to reach the database before the
        // new open row is inserted, or idx_politicalparty_symbol_current
        // rejects it.
        ArgumentCaptor<PoliticalPartySymbol> saved =
                ArgumentCaptor.forClass(PoliticalPartySymbol.class);
        InOrder ordered = inOrder(symbols, entityManager);
        ordered.verify(symbols).save(inUse);
        ordered.verify(entityManager).flush();
        ordered.verify(symbols).save(saved.capture());
        assertTrue(saved.getValue().isCurrent());
    }

    @Test
    void adoptRejectsADateTheSymbolInUseAlreadyCovers() {
        givenParty();
        LocalDate cameIn = LocalDate.of(2012, 4, 16);
        givenCurrentSymbol("Key", cameIn);

        IllegalArgumentException sameDay = assertThrows(IllegalArgumentException.class,
                () -> service.adopt(PARTY_ID, new AdoptSymbolRequest("Crown", null, cameIn)));
        IllegalArgumentException earlier = assertThrows(IllegalArgumentException.class,
                () -> service.adopt(PARTY_ID,
                        new AdoptSymbolRequest("Crown", null, cameIn.minusDays(1))));

        assertTrue(sameDay.getMessage().contains("must fall after"));
        assertTrue(earlier.getMessage().contains("must fall after"));
        verify(symbols, never()).save(any());
        verify(entityManager, never()).flush();
    }

    // V20 permits a symbol with no recorded start, so there is nothing to
    // compare a successor's date against.
    @Test
    void adoptAcceptsAnyDateWhenTheSymbolInUseHasNoRecordedStart() {
        givenParty();
        PoliticalPartySymbol inUse = givenCurrentSymbol("Key", null);
        savePassesThrough();

        LocalDate cameIn = LocalDate.of(2015, 3, 1);
        service.adopt(PARTY_ID, new AdoptSymbolRequest("Crown", null, cameIn));

        assertEquals(cameIn.minusDays(1), inUse.getUptoDate());
    }

    @Test
    void adoptRejectsUnknownParty() {
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.adopt(999L, new AdoptSymbolRequest("Crown", null, null)));

        assertTrue(thrown.getMessage().contains("Political party not found"));
        verify(symbols, never()).save(any());
    }

    @Test
    void currentSymbolIsEmptyForAPartyTheRegisterGaveNone() {
        givenParty();
        givenNoCurrentSymbol();

        assertTrue(service.findCurrent(PARTY_ID).isEmpty());
    }

    @Test
    void historyKeepsClosedSymbolsAlongsideTheOneInUse() {
        givenParty();
        PoliticalPartySymbol closed = symbol("Key", LocalDate.of(2012, 4, 16));
        closed.end(LocalDate.of(2022, 1, 9));
        PoliticalPartySymbol open = symbol("Crown", LocalDate.of(2022, 1, 10));
        when(symbols.findByParty(PARTY_ID)).thenReturn(List.of(open, closed));

        List<PoliticalPartySymbolDto> history = service.findHistory(PARTY_ID);

        assertEquals(2, history.size());
        assertTrue(history.get(0).current());
        assertFalse(history.get(1).current());
        assertEquals(LocalDate.of(2022, 1, 9), history.get(1).uptoDate());
    }

    @Test
    void historyRejectsAnUnknownParty() {
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findHistory(999L));
        verify(symbols, never()).findByParty(999L);
    }

    private void givenParty() {
        PoliticalParty party = partyEntity();
        when(politicalParties.findById(PARTY_ID)).thenReturn(Optional.of(party));
    }

    /** A real entity, so closing a symbol is exercised rather than stubbed. */
    private PoliticalPartySymbol givenCurrentSymbol(String description, LocalDate fromDate) {
        PoliticalPartySymbol current = symbol(description, fromDate);
        when(symbols.findCurrentByParty(PARTY_ID)).thenReturn(Optional.of(current));
        return current;
    }

    private void givenNoCurrentSymbol() {
        when(symbols.findCurrentByParty(PARTY_ID)).thenReturn(Optional.empty());
    }

    private static PoliticalPartySymbol symbol(String description, LocalDate fromDate) {
        return new PoliticalPartySymbol(partyEntity(), description, null, fromDate);
    }

    // Rejection paths never read the name; lenient() keeps strict stubs happy.
    private static PoliticalParty partyEntity() {
        PoliticalParty party = mock(PoliticalParty.class);
        lenient().when(party.getId()).thenReturn(PARTY_ID);
        lenient().when(party.getName()).thenReturn("Kenya National Congress");
        return party;
    }

    private void savePassesThrough() {
        when(symbols.save(any(PoliticalPartySymbol.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
