package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.CreateElectoralAreaRequest;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.ElectoralAreaDto;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.AreaType;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralArea;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.AreaTypeRepository;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.ElectoralAreaRepository;
import jakarta.data.page.PageRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the app-enforced hierarchy invariants: the schema comments
 * delegate parent-type checking and ancestor_path maintenance to this service,
 * so nothing but these tests catches a regression.
 */
@ExtendWith(MockitoExtension.class)
class ElectoralAreaServiceTest {

    @Mock
    private ElectoralAreaRepository electoralAreas;

    @Mock
    private AreaTypeRepository areaTypes;

    @InjectMocks
    private ElectoralAreaService service;

    @Test
    void createComputesAncestorPathFromParent() {
        givenAreaType("WARD");
        ElectoralArea parent = givenParent(42L, "/1/2/7/", "CONSTITUENCY");
        when(electoralAreas.findById(42L)).thenReturn(Optional.of(parent));
        when(electoralAreas.save(any(ElectoralArea.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ElectoralAreaDto created = service.create(
                new CreateElectoralAreaRequest("Kibra", "W-001", "WARD", 42L));

        ArgumentCaptor<ElectoralArea> saved = ArgumentCaptor.forClass(ElectoralArea.class);
        verify(electoralAreas).save(saved.capture());
        assertEquals("/1/2/7/42/", saved.getValue().getAncestorPath());
        assertEquals("Kibra", created.name());
        assertEquals("WARD", created.areaType());
        assertEquals(42L, created.parentId());
    }

    @ParameterizedTest
    @CsvSource({
            "COUNTRY, WORLD",
            "COUNTY, COUNTRY",
            "CONSTITUENCY, COUNTY",
            "WARD, CONSTITUENCY",
            "REGISTRATION_CENTER, WARD",
            "POLLING_STATION, REGISTRATION_CENTER"})
    void createAcceptsEachTypeUnderItsDesignatedParentType(String childType, String parentType) {
        givenAreaType(childType);
        ElectoralArea parent = givenParent(10L, "/1/", parentType);
        when(electoralAreas.findById(10L)).thenReturn(Optional.of(parent));
        when(electoralAreas.save(any(ElectoralArea.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ElectoralAreaDto created = service.create(
                new CreateElectoralAreaRequest("Area", null, childType, 10L));

        assertEquals(childType, created.areaType());
        ArgumentCaptor<ElectoralArea> saved = ArgumentCaptor.forClass(ElectoralArea.class);
        verify(electoralAreas).save(saved.capture());
        assertEquals("/1/10/", saved.getValue().getAncestorPath());
    }

    @Test
    void createRejectsUnknownAreaType() {
        when(areaTypes.findByName("PROVINCE")).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateElectoralAreaRequest("Nyanza", null, "PROVINCE", 1L)));

        assertTrue(thrown.getMessage().contains("Unknown area type"));
        verify(electoralAreas, never()).save(any());
    }

    @Test
    void createRejectsTheSeededRootType() {
        givenAreaType("WORLD");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateElectoralAreaRequest("Earth", null, "WORLD", 1L)));

        assertTrue(thrown.getMessage().contains("cannot be created"));
        verify(electoralAreas, never()).save(any());
    }

    @Test
    void createRejectsMissingParent() {
        givenAreaType("COUNTY");
        when(electoralAreas.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateElectoralAreaRequest("Nairobi", null, "COUNTY", 999L)));

        assertTrue(thrown.getMessage().contains("Parent area not found"));
        verify(electoralAreas, never()).save(any());
    }

    @Test
    void createRejectsParentOfTheWrongType() {
        givenAreaType("WARD");
        ElectoralArea county = givenParent(3L, "/1/2/", "COUNTY");
        when(electoralAreas.findById(3L)).thenReturn(Optional.of(county));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateElectoralAreaRequest("Kibra", null, "WARD", 3L)));

        assertTrue(thrown.getMessage().contains("must belong to a CONSTITUENCY"));
        verify(electoralAreas, never()).save(any());
    }

    @Test
    void subtreeQueriesByOwnPathPlusIdPrefix() {
        ElectoralArea area = mock(ElectoralArea.class);
        when(area.getId()).thenReturn(5L);
        when(area.getAncestorPath()).thenReturn("/1/2/");
        when(electoralAreas.findById(5L)).thenReturn(Optional.of(area));
        when(electoralAreas.findDescendants(anyString(), any(PageRequest.class)))
                .thenReturn(List.of());

        Optional<List<ElectoralAreaDto>> result = service.findDescendants(5L, 0, 20);

        assertTrue(result.isPresent());
        ArgumentCaptor<String> prefix = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PageRequest> pageRequest = ArgumentCaptor.forClass(PageRequest.class);
        verify(electoralAreas).findDescendants(prefix.capture(), pageRequest.capture());
        assertEquals("/1/2/5/%", prefix.getValue());
        // REST layer is 0-based, Jakarta Data is 1-based.
        assertEquals(1L, pageRequest.getValue().page());
        assertEquals(20, pageRequest.getValue().size());
    }

    @Test
    void subtreeOfTheRootUsesTheBareRootPath() {
        ElectoralArea root = mock(ElectoralArea.class);
        when(root.getId()).thenReturn(1L);
        when(root.getAncestorPath()).thenReturn("/");
        when(electoralAreas.findById(1L)).thenReturn(Optional.of(root));
        when(electoralAreas.findDescendants(anyString(), any(PageRequest.class)))
                .thenReturn(List.of());

        service.findDescendants(1L, 0, 100);

        ArgumentCaptor<String> prefix = ArgumentCaptor.forClass(String.class);
        verify(electoralAreas).findDescendants(prefix.capture(), any(PageRequest.class));
        assertEquals("/1/%", prefix.getValue());
    }

    @Test
    void subtreeOfUnknownAreaIsEmptyAndQueriesNothing() {
        when(electoralAreas.findById(999L)).thenReturn(Optional.empty());

        assertTrue(service.findDescendants(999L, 0, 20).isEmpty());
        verify(electoralAreas, never()).findDescendants(anyString(), any(PageRequest.class));
    }

    private void givenAreaType(String name) {
        AreaType type = mock(AreaType.class);
        when(type.getName()).thenReturn(name);
        when(areaTypes.findByName(name)).thenReturn(Optional.of(type));
    }

    private ElectoralArea givenParent(long id, String ancestorPath, String typeName) {
        AreaType parentType = mock(AreaType.class);
        when(parentType.getName()).thenReturn(typeName);
        ElectoralArea parent = mock(ElectoralArea.class);
        when(parent.getId()).thenReturn(id);
        // Rejection tests never read the path; lenient() keeps strict stubs happy.
        lenient().when(parent.getAncestorPath()).thenReturn(ancestorPath);
        when(parent.getAreaType()).thenReturn(parentType);
        return parent;
    }
}
