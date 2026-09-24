package com.arriyiaconsulting.siasaleo.service.domain.election.repository;

import com.arriyiaconsulting.siasaleo.service.domain.election.dto.RegisterAssignmentDtos.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.SqlValues.*;

@ApplicationScoped
public class RegisterAssignmentRepository extends NativeQueries {
    private AssignmentDto map(Object[] r) { return new AssignmentDto(number(r[0]),number(r[1]),number(r[2]),number(r[3]),text(r[4]),text(r[5])); }
    public Optional<AssignmentDto> current(long event) {
        return rows("SELECT a.id,a.election_event_id,a.register_id,a.supersedes_id,a.source_reference,cast(a.assigned_at as text) FROM election_register_assignment a WHERE a.election_event_id=?1 AND NOT EXISTS (SELECT 1 FROM election_register_assignment n WHERE n.supersedes_id=a.id)",event).stream().findFirst().map(this::map);
    }
    public List<AssignmentDto> list(long event,int offset,int size) {
        return rows("SELECT id,election_event_id,register_id,supersedes_id,source_reference,cast(assigned_at as text) FROM election_register_assignment WHERE election_event_id=?1 ORDER BY id DESC LIMIT ?2 OFFSET ?3",event,size,offset).stream().map(this::map).toList();
    }
}
