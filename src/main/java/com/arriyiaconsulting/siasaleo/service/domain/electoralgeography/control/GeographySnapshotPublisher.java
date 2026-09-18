package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.control;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto.GeographySnapshotDto;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralGeographySnapshot;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.mapping.GeographySnapshotMapper;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GeographySnapshotPublisher {
    @Inject private GeographySnapshotService service;
    @Inject private ElectoralAreaSnapshotRepository areas;
    @Inject private GeographySnapshotRepository snapshots;
    @Inject private GeographySnapshotMapper mapper;

    @Transactional
    public GeographySnapshotDto publish(Long id) {
        ElectoralGeographySnapshot snapshot = service.lockDraft(id);
        SnapshotHierarchy.validate(id, areas.all(id));
        snapshot.publish();
        snapshots.flush();
        return mapper.toDto(snapshot);
    }
}
