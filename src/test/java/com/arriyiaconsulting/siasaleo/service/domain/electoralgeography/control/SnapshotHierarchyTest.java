package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SnapshotHierarchyTest {
    private List<ElectoralAreaSnapshot> tree() throws Exception {
        var result = new ArrayList<ElectoralAreaSnapshot>();
        String path = "/";
        String[] names = {"WORLD", "COUNTRY", "COUNTY", "CONSTITUENCY", "WARD", "REGISTRATION_CENTER", "POLLING_STATION"};
        for (int i = 0; i < names.length; i++) {
            AreaType type = mock(AreaType.class);
            when(type.getName()).thenReturn(names[i]);
            var node = new ElectoralAreaSnapshot(1L, type, names[i], "001", i == 0 ? null : (long) i, path, null);
            set(node, "id", (long) i + 1);
            result.add(node);
            path += (i + 1) + "/";
        }
        return result;
    }
    private void set(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
    @Test void acceptsCompleteTreeRegardlessOfInputOrder() throws Exception {
        var nodes = tree();
        Collections.reverse(nodes);
        assertDoesNotThrow(() -> SnapshotHierarchy.validate(1L, nodes));
    }
    @Test void rejectsEmptyTreeMissingParentAndWrongSnapshot() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> SnapshotHierarchy.validate(1L, List.of()));
        var nodes = tree();
        assertThrows(IllegalArgumentException.class, () -> SnapshotHierarchy.validate(2L, nodes));
        nodes.remove(2);
        assertThrows(IllegalArgumentException.class, () -> SnapshotHierarchy.validate(1L, nodes));
    }
    @Test void rejectsStalePathsAndCycles() throws Exception {
        var nodes = tree();
        set(nodes.get(6), "ancestorPath", "/999/");
        assertThrows(IllegalArgumentException.class, () -> SnapshotHierarchy.validate(1L, nodes));
        var cycle = tree();
        set(cycle.get(4), "parentId", 7L);
        assertThrows(IllegalArgumentException.class, () -> SnapshotHierarchy.validate(1L, cycle));
    }
}
