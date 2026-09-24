package com.arriyiaconsulting.siasaleo.service.domain.election.control;

import org.mapstruct.factory.Mappers;
import com.arriyiaconsulting.siasaleo.service.domain.election.mapping.ElectionMapper;
import com.arriyiaconsulting.siasaleo.service.domain.election.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.repository.*;
import jakarta.data.page.PageRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElectionServiceTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {2013, 2022, 2027})
    void electionAgeDoesNotChooseASeparateCreationModel(int year) {
        ElectionCycle cycle = mock(ElectionCycle.class);
        when(cycle.getFromYear()).thenReturn(year);
        when(cycle.getUptoYear()).thenReturn(year + 5);
        when(cycles.findById(1L)).thenReturn(Optional.of(cycle));
        when(types.findById(1L)).thenReturn(Optional.of(mock(ElectionType.class)));
        when(statuses.findByStatusName("SCHEDULED")).thenReturn(Optional.of(mock(ElectionStatus.class)));
        when(events.save(any())).thenAnswer(i -> i.getArgument(0));
        LocalDate date = LocalDate.of(year, 8, 9);
        ElectionEventDto result = service.createEvent(new CreateElectionEventRequest(1L, date, 1L));
        assertEquals(date, result.electionDate());
        verify(events).save(any(ElectionEvent.class));
    }

    @Test
    void importsDocumentedStatusThroughTheSameEventService() {
        ElectionCycle cycle = cycle();
        when(types.findById(1L)).thenReturn(Optional.of(mock(ElectionType.class)));
        ElectionStatus completed = mock(ElectionStatus.class);
        when(statuses.findById(3L)).thenReturn(Optional.of(completed));
        when(events.save(any())).thenAnswer(i -> i.getArgument(0));
        service.createEvent(new CreateElectionEventRequest(1L, LocalDate.of(2027,8,9),1L,null,3L,"official notice"));
        ArgumentCaptor<ElectionEvent> saved = ArgumentCaptor.forClass(ElectionEvent.class);
        verify(events).save(saved.capture());
        assertSame(completed,saved.getValue().getStatus());
        assertEquals("official notice",saved.getValue().getSourceReference());
    }

    @Spy
    private ElectionMapper electionMapper = Mappers.getMapper(ElectionMapper.class);

    @Mock ElectionCycleRepository cycles;
    @Mock ElectionTypeRepository types;
    @Mock ElectionStatusRepository statuses;
    @Mock ElectionEventRepository events;
    @InjectMocks ElectionService service;

    @Test
    void createsScheduledEventAtInclusiveStartOfCycle() {
        ElectionCycle cycle = cycle();
        ElectionType type = mock(ElectionType.class);
        ElectionStatus status = mock(ElectionStatus.class);
        when(types.findById(2L)).thenReturn(Optional.of(type));
        when(statuses.findByStatusName("SCHEDULED")).thenReturn(Optional.of(status));
        when(events.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDate date = LocalDate.of(2027, 1, 1);

        ElectionEventDto result = service.createEvent(new CreateElectionEventRequest(1L, date, 2L));

        assertEquals(date, result.electionDate());
        ArgumentCaptor<ElectionEvent> saved = ArgumentCaptor.forClass(ElectionEvent.class);
        verify(events).save(saved.capture());
        assertSame(cycle, saved.getValue().getElectionCycle());
        assertSame(type, saved.getValue().getType());
        assertSame(status, saved.getValue().getStatus());
    }

    @ParameterizedTest
    @CsvSource({"2026-12-31", "2032-01-01"})
    void rejectsDateOutsideCycle(LocalDate date) {
        ElectionCycle cycle = mock(ElectionCycle.class);
        when(cycle.getFromYear()).thenReturn(2027);
        if (date.getYear() >= 2027) when(cycle.getUptoYear()).thenReturn(2032);
        when(cycles.findById(1L)).thenReturn(Optional.of(cycle));
        assertThrows(IllegalArgumentException.class,
                () -> service.createEvent(new CreateElectionEventRequest(1L, date, 2L)));
        verifyNoInteractions(types, statuses, events);
    }

    @Test
    void rejectsUnknownCycleAndMissingInput() {
        assertThrows(IllegalArgumentException.class, () -> service.createEvent(null));
        assertThrows(IllegalArgumentException.class, () -> service.createEvent(
                new CreateElectionEventRequest(99L, LocalDate.of(2027, 8, 10), 2L)));
        verify(events, never()).save(any());
    }

    @Test
    void missingScheduledSeedIsDeploymentError() {
        cycle();
        when(types.findById(2L)).thenReturn(Optional.of(mock(ElectionType.class)));
        assertThrows(IllegalStateException.class, () -> service.createEvent(
                new CreateElectionEventRequest(1L, LocalDate.of(2027, 8, 10), 2L)));
        verify(events, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
        "SCHEDULED, ONGOING, true", "SCHEDULED, CANCELLED, true",
        "ONGOING, COMPLETED, true", "ONGOING, NULLIFIED, true",
        "COMPLETED, NULLIFIED, true", "SCHEDULED, COMPLETED, false",
        "SCHEDULED, NULLIFIED, false", "ONGOING, SCHEDULED, false",
        "ONGOING, CANCELLED, false", "COMPLETED, ONGOING, false",
        "CANCELLED, SCHEDULED, false", "NULLIFIED, ONGOING, false"
    })
    void enforcesLifecycle(String current, String target, boolean allowed) {
        ElectionStatus initial = mock(ElectionStatus.class);
        ElectionStatus next = mock(ElectionStatus.class);
        when(initial.getStatusName()).thenReturn(current);
        when(next.getStatusName()).thenReturn(target);
        ElectionEvent event = new ElectionEvent(mock(ElectionCycle.class),
                LocalDate.of(2027, 8, 10), mock(ElectionType.class), initial);
        when(events.findById(1L)).thenReturn(Optional.of(event));
        when(statuses.findById(2L)).thenReturn(Optional.of(next));
        if (allowed) {
            when(events.save(event)).thenReturn(event);
            assertEquals(target, service.changeStatus(1L, new ChangeElectionStatusRequest(2L))
                    .orElseThrow().status().statusName());
            assertSame(next, event.getStatus());
        } else {
            assertThrows(IllegalArgumentException.class,
                    () -> service.changeStatus(1L, new ChangeElectionStatusRequest(2L)));
            assertSame(initial, event.getStatus());
            verify(events, never()).save(any());
        }
    }

    @Test
    void sameStatusIsIdempotent() {
        ElectionStatus status = mock(ElectionStatus.class);
        when(status.getStatusName()).thenReturn("CANCELLED");
        ElectionEvent event = new ElectionEvent(mock(ElectionCycle.class),
                LocalDate.of(2027, 8, 10), mock(ElectionType.class), status);
        when(events.findById(1L)).thenReturn(Optional.of(event));
        when(statuses.findById(2L)).thenReturn(Optional.of(status));
        assertTrue(service.changeStatus(1L, new ChangeElectionStatusRequest(2L)).isPresent());
        verify(events, never()).save(any());
    }

    @Test
    void absentEventHasNoStatusUpdate() {
        assertTrue(service.changeStatus(99L, new ChangeElectionStatusRequest(2L)).isEmpty());
        verifyNoInteractions(statuses);
    }

    @Test
    void listingBoundsPageSizeAndUsesOneBasedRepositoryPages() {
        when(events.search(isNull(), isNull(), isNull(), any())).thenReturn(List.of());
        assertEquals(List.of(), service.events(null, null, null, 2, 900));
        ArgumentCaptor<PageRequest> paging = ArgumentCaptor.forClass(PageRequest.class);
        verify(events).search(isNull(), isNull(), isNull(), paging.capture());
        assertEquals(3, paging.getValue().page());
        assertEquals(500, paging.getValue().size());
        assertThrows(IllegalArgumentException.class, () -> service.events(null, null, null, -1, 100));
    }

    private ElectionCycle cycle() {
        ElectionCycle cycle = mock(ElectionCycle.class);
        when(cycle.getFromYear()).thenReturn(2027);
        when(cycle.getUptoYear()).thenReturn(2032);
        when(cycles.findById(1L)).thenReturn(Optional.of(cycle));
        return cycle;
    }
}
