package com.arriyiaconsulting.siasaleo.service.domain.election.control;

import com.arriyiaconsulting.siasaleo.service.domain.election.dto.RegisterAssignmentDtos.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.repository.RegisterAssignmentRepository;
import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegisterService;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.PublicationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.*;
import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport.*;
import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.SqlValues.number;

@ApplicationScoped
public class RegisterAssignmentService {
    @Inject private RegisterAssignmentRepository repo;
    @Inject private VoterRegisterService registers;
    public Optional<AssignmentDto> current(long event) { return repo.current(event); }
    public List<AssignmentDto> assignments(long event,int page,int size) { return repo.list(event,offset(page,size),size(size)); }
    @Transactional
    public AssignmentDto assign(long event,AssignmentRequest r) {
        if(r==null || !positive(r.registerId()) || !text(r.sourceReference(),2048)) throw new IllegalArgumentException("Register and source reference are required");
        var events=repo.rows("SELECT id,geography_snapshot_id FROM election_event WHERE id=?1 FOR UPDATE",event);
        if(events.isEmpty()) throw new RecordNotFoundException();
        var register=registers.find(r.registerId(),true);
        if(register.status()!=PublicationStatus.PUBLISHED || !Objects.equals(register.geographySnapshotId(),number(events.getFirst()[1]))) throw new IllegalArgumentException("Register must be published in the election geography");
        Optional<AssignmentDto> current=repo.current(event);
        if(current.isPresent() && Objects.equals(current.get().registerId(),r.registerId()) && Objects.equals(current.get().supersedesId(),r.supersedesId()) && Objects.equals(current.get().sourceReference(),r.sourceReference())) return current.get();
        if(!Objects.equals(current.map(AssignmentDto::id).orElse(null),r.supersedesId())) throw new RecordConflictException("Assignment must supersede the current assignment");
        repo.insert("INSERT INTO election_register_assignment(election_event_id,register_id,supersedes_id,source_reference) VALUES(?1,?2,?3,?4) RETURNING id",event,r.registerId(),r.supersedesId(),r.sourceReference());
        return repo.current(event).orElseThrow();
    }
}
