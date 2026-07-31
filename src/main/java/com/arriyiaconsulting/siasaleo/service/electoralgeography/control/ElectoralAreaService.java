package com.arriyiaconsulting.siasaleo.service.electoralgeography.control;

import com.arriyiaconsulting.siasaleo.service.electoralgeography.dto.AreaTypeDto;
import com.arriyiaconsulting.siasaleo.service.electoralgeography.dto.CreateElectoralAreaRequest;
import com.arriyiaconsulting.siasaleo.service.electoralgeography.dto.ElectoralAreaDto;
import com.arriyiaconsulting.siasaleo.service.electoralgeography.entity.AreaType;
import com.arriyiaconsulting.siasaleo.service.electoralgeography.entity.ElectoralArea;
import com.arriyiaconsulting.siasaleo.service.electoralgeography.repository.AreaTypeRepository;
import com.arriyiaconsulting.siasaleo.service.electoralgeography.repository.ElectoralAreaRepository;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Business rules for the electoral-area hierarchy. Owns the app-enforced
 * invariants the schema comments delegate to the application: each area type
 * hangs under its designated parent type, and ancestor_path is always
 * parent.ancestorPath + parent.id + '/'.
 */
@ApplicationScoped
public class ElectoralAreaService {

    // Required parent type for each creatable area type; WORLD is absent
    // because the root is seeded, never created through the API.
    private static final Map<String, String> PARENT_TYPE = Map.of(
            "COUNTRY", "WORLD",
            "COUNTY", "COUNTRY",
            "CONSTITUENCY", "COUNTY",
            "WARD", "CONSTITUENCY",
            "REGISTRATION_CENTER", "WARD",
            "POLLING_STATION", "REGISTRATION_CENTER");

    private ElectoralAreaRepository electoralAreas;
    private AreaTypeRepository areaTypes;

    ElectoralAreaService() {
    }

    @Inject
    public ElectoralAreaService(ElectoralAreaRepository electoralAreas, AreaTypeRepository areaTypes) {
        this.electoralAreas = electoralAreas;
        this.areaTypes = areaTypes;
    }

    @Transactional
    public ElectoralAreaDto create(CreateElectoralAreaRequest request) {
        AreaType type = areaTypes.findByName(request.areaType())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown area type: " + request.areaType()));

        String requiredParentType = PARENT_TYPE.get(type.getName());
        if (requiredParentType == null) {
            throw new IllegalArgumentException(
                    "Areas of type " + type.getName() + " cannot be created");
        }

        ElectoralArea parent = electoralAreas.findById(request.parentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Parent area not found: " + request.parentId()));
        if (!requiredParentType.equals(parent.getAreaType().getName())) {
            throw new IllegalArgumentException(
                    "A " + type.getName() + " must belong to a " + requiredParentType
                    + ", but parent " + parent.getId() + " is a "
                    + parent.getAreaType().getName());
        }

        String ancestorPath = parent.getAncestorPath() + parent.getId() + "/";
        ElectoralArea area = electoralAreas.save(new ElectoralArea(
                request.name(), request.areaCode(), type, parent, ancestorPath));
        return ElectoralAreaDto.from(area);
    }

    public Optional<ElectoralAreaDto> findById(Long id) {
        return electoralAreas.findById(id).map(ElectoralAreaDto::from);
    }

    public List<ElectoralAreaDto> findByType(String typeName, int page, int size) {
        areaTypes.findByName(typeName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown area type: " + typeName));
        return electoralAreas.findByTypeName(typeName, pageRequest(page, size)).stream()
                .map(ElectoralAreaDto::from)
                .toList();
    }

    public Optional<List<ElectoralAreaDto>> findChildren(Long parentId) {
        return electoralAreas.findById(parentId)
                .map(parent -> electoralAreas.findChildren(parent.getId()).stream()
                        .map(ElectoralAreaDto::from)
                        .toList());
    }

    public Optional<List<ElectoralAreaDto>> findDescendants(Long id, int page, int size) {
        return electoralAreas.findById(id)
                .map(area -> area.getAncestorPath() + area.getId() + "/%")
                .map(prefix -> electoralAreas.findDescendants(prefix, pageRequest(page, size)).stream()
                        .map(ElectoralAreaDto::from)
                        .toList());
    }

    public List<AreaTypeDto> findAllAreaTypes() {
        return areaTypes.findAllOrdered().stream()
                .map(AreaTypeDto::from)
                .toList();
    }

    // Jakarta Data pages are 1-based; the REST layer exposes 0-based pages.
    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.ofPage(page + 1L).size(size).withoutTotal();
    }
}
