package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralAreaSnapshot;
import java.util.*;

/** Shared structural checks for creation and publication. */
final class SnapshotHierarchy {
    private SnapshotHierarchy() {}

    private static final Map<String, String> PARENTS = Map.of(
            "COUNTRY", "WORLD", "COUNTY", "COUNTRY", "CONSTITUENCY", "COUNTY",
            "WARD", "CONSTITUENCY", "REGISTRATION_CENTER", "WARD", "POLLING_STATION", "REGISTRATION_CENTER");

    static String path(Long snapshotId, String type, ElectoralAreaSnapshot parent) {
        if (type.equals("WORLD")) {
            if (parent != null) throw new IllegalArgumentException("WORLD must be the root");
            return "/";
        }
        if (parent == null || !snapshotId.equals(parent.getSnapshotId())
                || !Objects.equals(PARENTS.get(type), parent.getAreaType().getName())) {
            throw new IllegalArgumentException("Invalid parent for " + type + " in snapshot " + snapshotId);
        }
        return parent.getAncestorPath() + parent.getId() + "/";
    }

    static void validate(Long snapshotId, List<ElectoralAreaSnapshot> areas) {
        Map<Long, ElectoralAreaSnapshot> byId = new HashMap<>();
        for (ElectoralAreaSnapshot area : areas) {
            if (!snapshotId.equals(area.getSnapshotId()) || byId.put(area.getId(), area) != null) {
                throw new IllegalArgumentException("Mixed snapshots or duplicate area IDs");
            }
        }
        long roots = 0;
        long stations = 0;
        Set<List<Object>> codes = new HashSet<>();
        for (ElectoralAreaSnapshot area : areas) {
            String type = area.getAreaType().getName();
            ElectoralAreaSnapshot parent = byId.get(area.getParentId());
            if (area.getParentId() != null && parent == null) {
                throw new IllegalArgumentException("Missing historical parent");
            }
            if (!path(snapshotId, type, parent).equals(area.getAncestorPath())) {
                throw new IllegalArgumentException("Invalid ancestor path for area " + area.getId());
            }
            // Strict parent type progression also makes cycles impossible.
            if (area.getParentId() == null) roots++;
            if (type.equals("POLLING_STATION")) stations++;
            if (area.getAreaCode() != null && !codes.add(Arrays.asList(area.getParentId(), type, area.getAreaCode()))) {
                throw new IllegalArgumentException("Duplicate sibling area code: " + area.getAreaCode());
            }
        }
        if (roots != 1 || stations == 0) {
            throw new IllegalArgumentException("Publication requires one root and at least one polling station");
        }
    }
}
