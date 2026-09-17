package com.arriyiaconsulting.siasaleo.service.domain.election.control;

import com.arriyiaconsulting.siasaleo.service.domain.election.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.office.entity.Seat;
import com.arriyiaconsulting.siasaleo.service.domain.office.repository.SeatRepository;
import jakarta.data.page.PageRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContestServiceTest {
    @Mock ContestRepository contests;
    @Mock ElectionEventRepository events;
    @Mock SeatRepository seats;
    @InjectMocks ContestService service;

    @Test
    void createsContestWithStableForeignKeysAndTrimmedDescription() {
        references();
        when(contests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ContestDto result = service.create(new CreateContestRequest(1L, 2L, " MP race "));
        assertEquals(1L, result.electionEventId());
        assertEquals(2L, result.seatId());
        assertEquals("MP race", result.description());
    }

    @Test
    void rejectsDuplicateEventSeatPair() {
        references();
        when(contests.findByEventAndSeat(1L, 2L)).thenReturn(Optional.of(new Contest(1L, 2L, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateContestRequest(1L, 2L, null)));
        verify(contests, never()).save(any());
    }

    @Test
    void rejectsMissingEvent() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateContestRequest(1L, 2L, null)));
        verifyNoInteractions(seats, contests);
    }

    @Test
    void rejectsMissingSeat() {
        when(events.findById(1L)).thenReturn(Optional.of(mock(ElectionEvent.class)));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateContestRequest(1L, 2L, null)));
        verifyNoInteractions(contests);
    }

    @Test
    void rejectsMissingBodyAndOversizedDescriptionBeforePersistence() {
        assertThrows(IllegalArgumentException.class, () -> service.create(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateContestRequest(1L, 2L, "x".repeat(256))));
        verifyNoInteractions(events, seats, contests);
    }

    @Test
    void listingRequiresEventAndNonnegativePage() {
        assertThrows(IllegalArgumentException.class, () -> service.findByEvent(null, null, 0, 100));
        assertThrows(IllegalArgumentException.class, () -> service.findByEvent(1L, null, -1, 100));
        verifyNoInteractions(events, seats, contests);
    }

    @Test
    void listsOnlyRequestedEventAndSeatWithBoundedPagination() {
        references();
        when(contests.search(eq(1L), eq(2L), any())).thenReturn(List.of(new Contest(1L, 2L, null)));
        assertEquals(1, service.findByEvent(1L, 2L, 0, 0).size());
        ArgumentCaptor<PageRequest> paging = ArgumentCaptor.forClass(PageRequest.class);
        verify(contests).search(eq(1L), eq(2L), paging.capture());
        assertEquals(1, paging.getValue().page());
        assertEquals(1, paging.getValue().size());
    }

    private void references() {
        when(events.findById(1L)).thenReturn(Optional.of(mock(ElectionEvent.class)));
        when(seats.findById(2L)).thenReturn(Optional.of(mock(Seat.class)));
    }
}
