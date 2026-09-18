package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.mapping.GeographySnapshotMapper;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnapshotAreaServiceTest {
    @Mock GeographySnapshotService snapshots;
    @Mock GeographySnapshotRepository snapshotRepository;
    @Mock ElectoralAreaSnapshotRepository areas;
    @Mock ElectoralAreaRepository operationalAreas;
    @Mock AreaTypeRepository types;
    @Spy GeographySnapshotMapper mapper = Mappers.getMapper(GeographySnapshotMapper.class);
    @InjectMocks SnapshotAreaService service;

    private AreaType type(String name, long id) {
        var type = mock(AreaType.class);
        lenient().when(type.getName()).thenReturn(name);
        lenient().when(type.getId()).thenReturn(id);
        return type;
    }

    private ElectoralAreaSnapshot area(long id, String typeName) throws Exception {
        var area = new ElectoralAreaSnapshot(1L, type(typeName, 5), "Historical ward", "001", 3L, "/1/2/3/", null);
        var field = ElectoralAreaSnapshot.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(area, id);
        return area;
    }

    @Test void createUsesHistoricalParentAndPreservesLeadingZeros() throws Exception {
        var parent = area(4, "WARD");
        when(areas.find(1L, 4L)).thenReturn(Optional.of(parent));
        var centreType = type("REGISTRATION_CENTER", 6);
        when(types.findByName("REGISTRATION_CENTER")).thenReturn(Optional.of(centreType));
        when(areas.insert(any(ElectoralAreaSnapshot.class))).thenAnswer(c -> c.getArgument(0));
        var result = service.create(1L, new CreateSnapshotAreaRequest("Centre", "0007", "REGISTRATION_CENTER", 4L, "page:9"));
        assertEquals(1L, result.snapshotId());
        assertEquals("0007", result.areaCode());
        var capture = ArgumentCaptor.forClass(ElectoralAreaSnapshot.class);
        verify(areas).insert(capture.capture());
        assertEquals("/1/2/3/4/", capture.getValue().getAncestorPath());
        verify(snapshots).lockDraft(1L);
        verifyNoInteractions(operationalAreas);
    }

    @Test void otherSnapshotAreaCannotBeReadOrUsedAsParent() {
        when(areas.find(1L, 99L)).thenReturn(Optional.empty());
        assertThrows(GeographyNotFoundException.class, () -> service.find(1L, 99L, true));
        var wardType = type("WARD", 5);
        when(types.findByName("WARD")).thenReturn(Optional.of(wardType));
        assertThrows(GeographyNotFoundException.class, () -> service.create(1L,
                new CreateSnapshotAreaRequest("Ward", "001", "WARD", 99L, null)));
        verify(areas, never()).insert(any(ElectoralAreaSnapshot.class));
    }

    @Test void publishedSnapshotRejectsWritesBeforeLookingUpNodes() {
        doThrow(new IllegalArgumentException("Published")).when(snapshots).lockDraft(1L);
        assertThrows(IllegalArgumentException.class, () -> service.update(1L, 7L,
                new UpdateSnapshotAreaRequest("Changed", "001", null)));
        assertThrows(IllegalArgumentException.class, () -> service.delete(1L, 7L));
        verifyNoInteractions(areas);
    }

    @Test void cannotDeleteParentAndDuplicateSiblingCodesAreRejected() throws Exception {
        var area = area(7, "WARD");
        when(areas.find(1L, 7L)).thenReturn(Optional.of(area));
        when(areas.children(1L, 7L, 0, 1)).thenReturn(List.of(area));
        assertThrows(IllegalArgumentException.class, () -> service.delete(1L, 7L));
        verify(areas, never()).delete(any());
        when(areas.duplicateCode(1L, 3L, 5L, "002", 7L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.update(1L, 7L,
                new UpdateSnapshotAreaRequest("Changed", "002", null)));
        assertEquals("001", area.getAreaCode());
    }

    @Test void subtreeIncludesSnapshotInQuery() throws Exception {
        var ward = area(7, "WARD");
        when(areas.find(1L, 7L)).thenReturn(Optional.of(ward));
        when(areas.descendants(1L, "/1/2/3/7/%", 0, 100)).thenReturn(List.of());
        assertTrue(service.descendants(1L, 7L, false, 0, 100).isEmpty());
        verify(snapshots).requireVisible(1L, false);
        verify(areas).descendants(1L, "/1/2/3/7/%", 0, 100);
    }
}
