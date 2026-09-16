package com.arriyiaconsulting.siasaleo.service.domain.office.control;

import com.arriyiaconsulting.siasaleo.service.domain.office.dto.OfficeDto;
import com.arriyiaconsulting.siasaleo.service.domain.office.entity.Office;
import com.arriyiaconsulting.siasaleo.service.domain.office.repository.OfficeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {

    @Mock
    private OfficeRepository offices;

    @InjectMocks
    private OfficeService service;

    @Test
    void listPreservesQueryOrderAndIncludesDeputiesAndBothLevels() {
        Office deputy = office(7L, "Deputy Governor", "DG", "County");
        Office president = office(1L, "President", "PRES", "National");
        when(president.getDescription()).thenReturn("Head of State and Government");
        when(offices.findAllOrderedByName()).thenReturn(List.of(deputy, president));

        assertEquals(List.of(
                new OfficeDto(7L, "Deputy Governor", "DG", null, "County"),
                new OfficeDto(1L, "President", "PRES", "Head of State and Government", "National")),
                service.findAll());
    }

    @Test
    void emptyRegisterReturnsEmptyList() {
        when(offices.findAllOrderedByName()).thenReturn(List.of());
        assertEquals(List.of(), service.findAll());
    }

    @Test
    void findByIdMapsOfficeWithNullableDescription() {
        Office governor = office(6L, "Governor", "GOV", "County");
        when(offices.findById(6L)).thenReturn(Optional.of(governor));
        assertEquals(new OfficeDto(6L, "Governor", "GOV", null, "County"),
                service.findById(6L).orElseThrow());
    }

    @Test
    void unknownOfficeIsAbsent() {
        when(offices.findById(999L)).thenReturn(Optional.empty());
        assertTrue(service.findById(999L).isEmpty());
    }

    private static Office office(long id, String name, String abbreviation, String level) {
        Office office = mock(Office.class);
        when(office.getId()).thenReturn(id);
        when(office.getName()).thenReturn(name);
        when(office.getAbbreviation()).thenReturn(abbreviation);
        when(office.getLevel()).thenReturn(level);
        return office;
    }
}
