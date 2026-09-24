package com.arriyiaconsulting.siasaleo.service.domain.voter.control;

import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegisterDtos.*;
import com.arriyiaconsulting.siasaleo.service.domain.voter.repository.VoterRegisterRepository;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport.ValidationReport;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.Coverage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.PublicationStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import java.util.*;
import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport.*;
import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.SqlValues.number;
import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.SqlValues.decimal;

/** Official register editions for any election; personal declarations are a separate voter capability. */
@ApplicationScoped
public class VoterRegisterService {
    @Inject private VoterRegisterRepository repo;
    @Inject private Validator validator;

    public RegisterDto find(long id,boolean admin) {
        RegisterDto r=repo.find(id,false).orElseThrow(RecordNotFoundException::new);
        if(!admin && r.status()!=PublicationStatus.PUBLISHED) throw new RecordNotFoundException();
        return r;
    }
    public List<RegisterDto> registers(boolean admin,int page,int size) { return repo.list(admin,offset(page,size),size(size)); }
    /**
     * Everything checkable without the database. Field rules are declared on
     * RegisterRequest and its counts; only rules spanning several rows live here.
     */
    public static List<String> shapeErrors(Validator validator,RegisterRequest r) {
        if(r==null) return List.of("Register body is required");
        // Before anything else, so an oversized payload is refused without
        // validating every one of its rows.
        if(list(r.counts()).size()>10000) return List.of("Use at most 10000 count rows per batch");
        List<String> e=new ArrayList<>(violations(validator,r));
        Set<Long> seen=new HashSet<>();
        for(RegisterCount c:list(r.counts())) {
            if(c!=null && c.areaId()!=null && !seen.add(c.areaId())) e.add("Duplicate register area: "+c.areaId());
        }
        return e;
    }
    public ValidationReport preview(RegisterRequest r) {
        List<String> e=new ArrayList<>(shapeErrors(validator,r));
        if(!e.isEmpty()) return new ValidationReport(false,e);
        if(!repo.exists("SELECT count(*) FROM electoral_geography_snapshot WHERE id=?1 AND status='PUBLISHED'",r.geographySnapshotId())) e.add("Published geography is required");
        if(!repo.exists("SELECT count(*) FROM electoral_area_snapshot WHERE id=?1 AND snapshot_id=?2",r.scopeAreaId(),r.geographySnapshotId())) e.add("Scope is outside the snapshot");
        if(!repo.exists("SELECT count(*) FROM electoral_area_snapshot WHERE snapshot_id=?1 AND area_type_id=?2 AND area_in_snapshot_scope(id,?3)",r.geographySnapshotId(),r.areaTypeId(),r.scopeAreaId())) e.add("No areas at requested granularity within scope");
        for(RegisterCount c:list(r.counts())) if(!repo.exists("SELECT count(*) FROM electoral_area_snapshot WHERE id=?1 AND snapshot_id=?2 AND area_type_id=?3 AND area_in_snapshot_scope(id,?4)",c.areaId(),r.geographySnapshotId(),r.areaTypeId(),r.scopeAreaId())) e.add("Invalid register area: "+c.areaId());
        if(r.supersedesId()!=null) {
            RegisterDto p=repo.find(r.supersedesId(),false).orElse(null);
            if(p==null || p.status()!=PublicationStatus.PUBLISHED || !Objects.equals(p.geographySnapshotId(),r.geographySnapshotId()) || !Objects.equals(p.scopeAreaId(),r.scopeAreaId())) e.add("Predecessor must be a published register in the same geography and scope");
        }
        return new ValidationReport(e.isEmpty(),e);
    }
    @Transactional
    public RegisterImportResult create(RegisterRequest r) {
        requireValid(preview(r));
        String digest=fingerprint(r);
        repo.scalar("SELECT 1 FROM pg_advisory_xact_lock(cast(hashtext(?1) as bigint))","register:"+r.geographySnapshotId()+":"+r.scopeAreaId()+":"+r.sourceSha256());
        var existing=repo.rows("SELECT id,import_fingerprint FROM voter_register WHERE geography_snapshot_id=?1 AND scope_area_id=?2 AND area_type_id=?3 AND source_sha256=?4",r.geographySnapshotId(),r.scopeAreaId(),r.areaTypeId(),r.sourceSha256());
        if(!existing.isEmpty()) {
            if(!digest.equals(existing.getFirst()[1])) throw new RecordConflictException("Source already has a different register payload; create a sourced revision");
            return result(number(existing.getFirst()[0]),true);
        }
        long id=repo.insert("INSERT INTO voter_register(geography_snapshot_id,scope_area_id,area_type_id,source_reference,source_sha256,coverage,supersedes_id,import_fingerprint) VALUES(?1,?2,?3,?4,?5,?6,?7,?8) RETURNING id",r.geographySnapshotId(),r.scopeAreaId(),r.areaTypeId(),r.sourceReference(),r.sourceSha256(),r.coverage(),r.supersedesId(),digest);
        insertCounts(id,r,digest);
        return result(id,false);
    }
    @Transactional
    public RegisterImportResult append(long id,RegisterRequest r) {
        RegisterDto p=repo.find(id,true).orElseThrow(RecordNotFoundException::new);
        requireContext(p,r);
        String digest=fingerprint(r);
        if(repo.exists("SELECT count(*) FROM voter_register_import_batch WHERE register_id=?1 AND fingerprint=?2",id,digest)) return result(id,true);
        if(p.status()!=PublicationStatus.DRAFT) throw new RecordConflictException("Published registers require a revision");
        requireValid(preview(r));
        insertCounts(id,r,digest);
        return result(id,false);
    }
    private void insertCounts(long id,RegisterRequest r,String digest) {
        for(RegisterCount c:list(r.counts())) repo.execute("INSERT INTO voter_register_count(register_id,area_id,registered_voters,source_record_reference) VALUES(?1,?2,?3,?4)",id,c.areaId(),c.registeredVoters(),c.sourceRecordReference());
        repo.execute("INSERT INTO voter_register_import_batch(register_id,fingerprint) VALUES(?1,?2)",id,digest);
    }
    public ValidationReport validation(long id) { find(id,true); var e=repo.errors(id); return new ValidationReport(e.isEmpty(),e); }
    @Transactional
    public RegisterDto publish(long id) {
        RegisterDto p=repo.find(id,true).orElseThrow(RecordNotFoundException::new);
        if(p.status()==PublicationStatus.PUBLISHED) return p;
        requireValid(validation(id));
        repo.execute("UPDATE voter_register SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP WHERE id=?1",id);
        return find(id,true);
    }
    @Transactional
    public void deleteDraft(long id) {
        if(repo.find(id,true).orElseThrow(RecordNotFoundException::new).status()!=PublicationStatus.DRAFT) throw new RecordConflictException("Published registers are immutable");
        repo.execute("DELETE FROM voter_register_count WHERE register_id=?1",id);
        repo.execute("DELETE FROM voter_register_import_batch WHERE register_id=?1",id);
        repo.execute("DELETE FROM voter_register WHERE id=?1",id);
    }
    public RegisterSummary summary(long id,Long area,boolean admin) {
        RegisterDto r=find(id,admin); long scope=scope(r,area);
        Object[] count=repo.rows("SELECT sum(registered_voters),count(*) FROM voter_register_count WHERE register_id=?1 AND area_in_snapshot_scope(area_id,?2)",id,scope).getFirst();
        long expected=repo.count("SELECT count(*) FROM electoral_area_snapshot WHERE snapshot_id=?1 AND area_type_id=?2 AND area_in_snapshot_scope(id,?3)",r.geographySnapshotId(),r.areaTypeId(),scope);
        long actual=number(count[1]);
        return new RegisterSummary(r,scope,decimal(count[0]),actual,expected,expected>0 && actual==expected);
    }
    public List<RegisterCount> counts(long id,Long area,boolean admin,int page,int size) {
        RegisterDto r=find(id,admin);
        return repo.rows("SELECT area_id,registered_voters,source_record_reference FROM voter_register_count WHERE register_id=?1 AND area_in_snapshot_scope(area_id,?2) ORDER BY area_id LIMIT ?3 OFFSET ?4",id,scope(r,area),size(size),offset(page,size)).stream()
            .map(row->new RegisterCount(number(row[0]),number(row[1]),(String)row[2])).toList();
    }
    private long scope(RegisterDto r,Long requested) {
        long id=requested==null?r.scopeAreaId():requested;
        if(!Boolean.TRUE.equals(repo.scalar("SELECT area_in_snapshot_scope(?1,?2)",id,r.scopeAreaId()))) throw new RecordNotFoundException();
        return id;
    }
    private RegisterImportResult result(long id,boolean replay) { return new RegisterImportResult(find(id,true),replay,validation(id)); }
    private void requireContext(RegisterDto p,RegisterRequest r) {
        if(r==null || !Objects.equals(p.geographySnapshotId(),r.geographySnapshotId()) || !Objects.equals(p.scopeAreaId(),r.scopeAreaId()) || !Objects.equals(p.areaTypeId(),r.areaTypeId()) || !Objects.equals(p.sourceReference(),r.sourceReference()) || !Objects.equals(p.sourceSha256(),r.sourceSha256()) || !p.coverage().name().equals(r.coverage()) || !Objects.equals(p.supersedesId(),r.supersedesId())) throw new IllegalArgumentException("Batch metadata must match its register");
    }
}
