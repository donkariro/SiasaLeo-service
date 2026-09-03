package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartyColorsDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.ReplaceColorsRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartyColor;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyColorRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import jakarta.persistence.EntityManager;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for colours, the one part of V20 with no validity period: they
 * are replaced wholesale rather than opened and closed, and display_order is
 * rewritten 1..n from the order the caller gives. Nothing in the schema
 * constrains the values, so repeats are stored rather than refused.
 */
@ExtendWith(MockitoExtension.class)
class PoliticalPartyColorServiceTest {

    private static final long PARTY_ID = 3L;

    @Mock
    private PoliticalPartyColorRepository colors;

    @Mock
    private PoliticalPartyRepository politicalParties;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PoliticalPartyColorService service;

    @Test
    void replaceNumbersTheColoursFromOneInTheOrderGiven() {
        givenParty();
        givenExistingColors();
        savePassesThrough();

        PoliticalPartyColorsDto replaced = service.replace(PARTY_ID,
                new ReplaceColorsRequest(List.of("Blue", "Green", "Yellow")));

        assertEquals(List.of("Blue", "Green", "Yellow"), replaced.colors());
        assertEquals(PARTY_ID, replaced.politicalPartyId());
        ArgumentCaptor<PoliticalPartyColor> saved =
                ArgumentCaptor.forClass(PoliticalPartyColor.class);
        verify(colors, times(3)).save(saved.capture());
        assertEquals(List.of(1, 2, 3), saved.getAllValues().stream()
                .map(PoliticalPartyColor::getDisplayOrder).toList());
        assertEquals(List.of("Blue", "Green", "Yellow"), saved.getAllValues().stream()
                .map(PoliticalPartyColor::getColorName).toList());
    }

    @Test
    void replaceTrimsEachColour() {
        givenParty();
        givenExistingColors();
        savePassesThrough();

        PoliticalPartyColorsDto replaced = service.replace(PARTY_ID,
                new ReplaceColorsRequest(List.of("  Blue  ", "Green")));

        assertEquals(List.of("Blue", "Green"), replaced.colors());
    }

    @Test
    void replaceRemovesTheOldColoursBeforeInsertingTheNewOnes() {
        givenParty();
        List<PoliticalPartyColor> existing = givenExistingColors(color("Red", 1),
                color("Black", 2));
        savePassesThrough();

        service.replace(PARTY_ID, new ReplaceColorsRequest(List.of("Blue")));

        InOrder ordered = inOrder(colors, entityManager);
        ordered.verify(colors).deleteAll(existing);
        ordered.verify(entityManager).flush();
        ordered.verify(colors).save(any(PoliticalPartyColor.class));
    }

    @Test
    void replaceSkipsTheDeleteWhenThePartyHasNoColoursYet() {
        givenParty();
        givenExistingColors();
        savePassesThrough();

        service.replace(PARTY_ID, new ReplaceColorsRequest(List.of("Blue")));

        verify(colors, never()).deleteAll(anyList());
        verify(entityManager, never()).flush();
    }

    // The register lists no colours for some parties, so clearing them is a
    // state the data needs to be able to reach.
    @Test
    void replaceWithAnEmptyListClearsTheColours() {
        givenParty();
        List<PoliticalPartyColor> existing = givenExistingColors(color("Red", 1));

        PoliticalPartyColorsDto replaced = service.replace(PARTY_ID,
                new ReplaceColorsRequest(List.of()));

        assertTrue(replaced.colors().isEmpty());
        verify(colors).deleteAll(existing);
        verify(colors, never()).save(any());
    }

    // Nothing in V20 makes colours unique, and V20 itself carried tokens like
    // 'Colourless' across verbatim; inventing a rule here would be wrong.
    @Test
    void replaceStoresRepeatedColoursRatherThanRefusingThem() {
        givenParty();
        givenExistingColors();
        savePassesThrough();

        PoliticalPartyColorsDto replaced = service.replace(PARTY_ID,
                new ReplaceColorsRequest(List.of("Black", "White", "Black")));

        assertEquals(List.of("Black", "White", "Black"), replaced.colors());
    }

    @Test
    void replaceRejectsUnknownParty() {
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.replace(999L, new ReplaceColorsRequest(List.of("Blue"))));

        assertTrue(thrown.getMessage().contains("Political party not found"));
        verify(colors, never()).save(any());
        verify(colors, never()).deleteAll(anyList());
    }

    @Test
    void findByPartyReturnsTheColoursInRegisterOrder() {
        givenParty();
        givenExistingColors(color("Red", 1), color("Black", 2), color("Green", 3));

        PoliticalPartyColorsDto found = service.findByParty(PARTY_ID);

        assertEquals(PARTY_ID, found.politicalPartyId());
        assertEquals(List.of("Red", "Black", "Green"), found.colors());
    }

    @Test
    void findByPartyIsEmptyForAPartyWithNoColoursRecorded() {
        givenParty();
        givenExistingColors();

        assertTrue(service.findByParty(PARTY_ID).colors().isEmpty());
    }

    @Test
    void findByPartyRejectsAnUnknownParty() {
        when(politicalParties.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findByParty(999L));
        verify(colors, never()).findByParty(anyLong());
    }

    private void givenParty() {
        PoliticalParty party = partyEntity();
        when(politicalParties.findById(PARTY_ID)).thenReturn(Optional.of(party));
    }

    private List<PoliticalPartyColor> givenExistingColors(PoliticalPartyColor... existing) {
        List<PoliticalPartyColor> found = List.of(existing);
        when(colors.findByParty(PARTY_ID)).thenReturn(found);
        return found;
    }

    private static PoliticalPartyColor color(String colorName, int displayOrder) {
        return new PoliticalPartyColor(partyEntity(), colorName, displayOrder);
    }

    private static PoliticalParty partyEntity() {
        PoliticalParty party = mock(PoliticalParty.class);
        lenient().when(party.getId()).thenReturn(PARTY_ID);
        return party;
    }

    private void savePassesThrough() {
        when(colors.save(any(PoliticalPartyColor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
