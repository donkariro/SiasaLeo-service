package com.arriyiaconsulting.siasaleo.service.domain.office.control;

import com.arriyiaconsulting.siasaleo.service.domain.office.entity.Office;
import com.arriyiaconsulting.siasaleo.service.domain.office.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralArea;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.ElectoralAreaRepository;
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
class SeatServiceTest {
    @Mock SeatRepository seats;
    @Mock OfficeRepository offices;
    @Mock ElectoralAreaRepository areas;
    @InjectMocks SeatService service;

    @Test
    void filtersByExactOfficeAndAreaWithBoundedPagination() {
        when(offices.findById(1L)).thenReturn(Optional.of(mock(Office.class)));
        when(areas.findById(2L)).thenReturn(Optional.of(mock(ElectoralArea.class)));
        when(seats.search(eq(1L), eq(2L), any())).thenReturn(List.of());
        assertEquals(List.of(), service.search(1L, 2L, 3, 1000));
        ArgumentCaptor<PageRequest> paging = ArgumentCaptor.forClass(PageRequest.class);
        verify(seats).search(eq(1L), eq(2L), paging.capture());
        assertEquals(4, paging.getValue().page());
        assertEquals(500, paging.getValue().size());
    }

    @Test
    void rejectsUnknownOffice() {
        assertThrows(IllegalArgumentException.class, () -> service.search(99L, null, 0, 100));
        verifyNoInteractions(seats, areas);
    }

    @Test
    void rejectsUnknownArea() {
        assertThrows(IllegalArgumentException.class, () -> service.search(null, 99L, 0, 100));
        verifyNoInteractions(seats, offices);
    }

    @Test
    void rejectsNegativePageBeforeReadingRepositories() {
        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, -1, 100));
        verifyNoInteractions(seats, offices, areas);
    }

    @Test
    void missingSeatIsAbsent() {
        assertTrue(service.findById(99L).isEmpty());
    }
}
