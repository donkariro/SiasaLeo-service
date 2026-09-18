package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import org.mapstruct.factory.Mappers;
import org.mockito.Spy;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.mapping.PoliticalPartyMapper;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.AdoptSloganRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartySloganDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartySlogan;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartySloganRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for slogans, which differ from symbols in the one way that
 * matters: V20 puts no unique index over them, so several may be in use at
 * once and adopting one must leave the rest alone. Retiring closes a row
 * rather than deleting it, cannot happen twice, and cannot predate the day the
 * slogan came in — except where the register never recorded that day.
 */
@ExtendWith(MockitoExtension.class)
class PoliticalPartySloganServiceTest {

    @Spy
    private PoliticalPartyMapper politicalPartyMapper = Mappers.getMapper(PoliticalPartyMapper.class);


    private static final long PARTY_ID = 3L;
    private static final long SLOGAN_ID = 21L;

    @Mock
    private PoliticalPartySloganRepository slogans;

    @Mock
    private PoliticalPartyRepository politicalParties;

    @InjectMocks
    private PoliticalPartySloganService service;

    @Test
    void adoptRecordsTheSloganAgainstTheParty() {
        givenParty();
        savePassesThrough();

        PoliticalPartySloganDto adopted = service.adopt(PARTY_ID,
                new AdoptSloganRequest("Wakenya Tujipange", null));

        assertEquals(PARTY_ID, adopted.politicalPartyId());
        assertEquals("Wakenya Tujipange", adopted.slogan());
        assertEquals(LocalDate.now(), adopted.fromDate());
        assertNull(adopted.uptoDate());
        assertTrue(adopted.current());
    }

    @Test
    void adoptKeepsABackfilledStartDate() {
        givenParty();
        savePassesThrough();

        LocalDate cameIn = LocalDate.of(2012, 4, 16);
        PoliticalPartySloganDto adopted = service.adopt(PARTY_ID,
                new AdoptSloganRequest("Wakenya Tujipange", cameIn));

        assertEquals(cameIn, adopted.fromDate());
    }

    @Test
    void adoptTrimsTheSlogan() {
        givenParty();
        savePassesThrough();

        PoliticalPartySloganDto adopted = service.adopt(PARTY_ID,
                new AdoptSloganRequest("  Wakenya Tujipange  ", null));

        assertEquals("Wakenya Tujipange", adopted.slogan());
    }

    // Unlike a symbol, adopting a slogan retires nothing: V20 has no unique
    // index, so a party may campaign under several at once.
    @Test
    void adoptLeavesTheSlogansAlreadyInUseAlone() {
        givenParty();
        savePassesThrough();

        service.adopt(PARTY_ID, new AdoptSloganRequest("Haki na Usawa", null));

        verify(slogans, never()).findCurrentByParty(anyLong());
        verify(slogans).save(any(PoliticalPartySlogan.class));
    }

    @Test
    void adoptRejectsUnknownParty() {
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.adopt(999L, new AdoptSloganRequest("Haki na Usawa", null)));

        assertTrue(thrown.getMessage().contains("Political party not found"));
        verify(slogans, never()).save(any());
    }

    @Test
    void retireClosesTheSloganOnTheGivenDay() {
        givenParty();
        PoliticalPartySlogan current = givenSlogan(LocalDate.of(2012, 4, 16));
        savePassesThrough();

        LocalDate retiredOn = LocalDate.of(2022, 8, 8);
        PoliticalPartySloganDto retired = service.retire(PARTY_ID, SLOGAN_ID, retiredOn);

        assertEquals(retiredOn, current.getUptoDate());
        assertEquals(retiredOn, retired.uptoDate());
        assertFalse(retired.current());
        verify(slogans).save(current);
    }

    @Test
    void retireDefaultsToToday() {
        givenParty();
        PoliticalPartySlogan current = givenSlogan(LocalDate.now().minusYears(3));
        savePassesThrough();

        service.retire(PARTY_ID, SLOGAN_ID, null);

        assertEquals(LocalDate.now(), current.getUptoDate());
    }

    // The V20 CHECK allows upto_date = from_date.
    @Test
    void retireAcceptsASingleDaySlogan() {
        givenParty();
        LocalDate cameIn = LocalDate.of(2022, 8, 8);
        PoliticalPartySlogan current = givenSlogan(cameIn);
        savePassesThrough();

        service.retire(PARTY_ID, SLOGAN_ID, cameIn);

        assertEquals(cameIn, current.getUptoDate());
    }

    @Test
    void retireRejectsADateBeforeTheSloganCameIn() {
        givenParty();
        LocalDate cameIn = LocalDate.of(2022, 8, 8);
        givenSlogan(cameIn);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.retire(PARTY_ID, SLOGAN_ID, cameIn.minusDays(1)));

        assertTrue(thrown.getMessage().contains("falls before"));
        verify(slogans, never()).save(any());
    }

    @Test
    void retireAcceptsAnyDateWhenTheSloganHasNoRecordedStart() {
        givenParty();
        PoliticalPartySlogan current = givenSlogan(null);
        savePassesThrough();

        LocalDate retiredOn = LocalDate.of(2015, 1, 1);
        service.retire(PARTY_ID, SLOGAN_ID, retiredOn);

        assertEquals(retiredOn, current.getUptoDate());
    }

    @Test
    void retireRejectsASloganAlreadyRetired() {
        givenParty();
        PoliticalPartySlogan closed = slogan(LocalDate.of(2012, 4, 16));
        closed.end(LocalDate.of(2022, 8, 8));
        when(slogans.findById(SLOGAN_ID)).thenReturn(Optional.of(closed));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.retire(PARTY_ID, SLOGAN_ID, null));

        assertTrue(thrown.getMessage().contains("already retired on 2022-08-08"));
        verify(slogans, never()).save(any());
    }

    // The party is part of the address, so a slogan cannot be retired through
    // the wrong one.
    @Test
    void retireRejectsASloganBelongingToAnotherParty() {
        givenParty();
        PoliticalPartySlogan elsewhere = new PoliticalPartySlogan(
                partyEntity(99L), "Kenya Mpya", LocalDate.of(2012, 4, 16));
        when(slogans.findById(SLOGAN_ID)).thenReturn(Optional.of(elsewhere));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.retire(PARTY_ID, SLOGAN_ID, null));

        assertTrue(thrown.getMessage().contains("does not belong to party"));
        verify(slogans, never()).save(any());
    }

    @Test
    void retireRejectsAnUnknownSlogan() {
        givenParty();
        when(slogans.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.retire(PARTY_ID, 999L, null));

        assertTrue(thrown.getMessage().contains("Slogan not found"));
        verify(slogans, never()).save(any());
    }

    @Test
    void listingDefaultsToTheSlogansInUse() {
        givenParty();
        when(slogans.findCurrentByParty(PARTY_ID)).thenReturn(List.of());

        service.findByParty(PARTY_ID, true);

        verify(slogans).findCurrentByParty(PARTY_ID);
        verify(slogans, never()).findByParty(anyLong());
    }

    @Test
    void listingWidensToEverySloganOnRecordWhenCurrentIsOff() {
        givenParty();
        PoliticalPartySlogan closed = slogan(LocalDate.of(2012, 4, 16));
        closed.end(LocalDate.of(2022, 8, 8));
        PoliticalPartySlogan open = slogan(LocalDate.of(2022, 8, 9));
        when(slogans.findByParty(PARTY_ID)).thenReturn(List.of(open, closed));

        List<PoliticalPartySloganDto> found = service.findByParty(PARTY_ID, false);

        assertEquals(2, found.size());
        assertTrue(found.get(0).current());
        assertFalse(found.get(1).current());
        verify(slogans, never()).findCurrentByParty(anyLong());
    }

    @Test
    void listingRejectsAnUnknownParty() {
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findByParty(999L, true));
        verify(slogans, never()).findCurrentByParty(anyLong());
    }

    private void givenParty() {
        PoliticalParty party = partyEntity(PARTY_ID);
        when(politicalParties.findById(PARTY_ID)).thenReturn(Optional.of(party));
    }

    /** A real entity, so closing a slogan is exercised rather than stubbed. */
    private PoliticalPartySlogan givenSlogan(LocalDate fromDate) {
        PoliticalPartySlogan current = slogan(fromDate);
        when(slogans.findById(SLOGAN_ID)).thenReturn(Optional.of(current));
        return current;
    }

    private static PoliticalPartySlogan slogan(LocalDate fromDate) {
        return new PoliticalPartySlogan(partyEntity(PARTY_ID), "Wakenya Tujipange", fromDate);
    }

    private static PoliticalParty partyEntity(long id) {
        PoliticalParty party = mock(PoliticalParty.class);
        lenient().when(party.getId()).thenReturn(id);
        return party;
    }

    private void savePassesThrough() {
        when(slogans.save(any(PoliticalPartySlogan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
