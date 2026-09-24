package com.arriyiaconsulting.siasaleo.service.domain.result.control;

import com.arriyiaconsulting.siasaleo.service.domain.result.dto.ResultDtos.*;
import com.arriyiaconsulting.siasaleo.service.domain.result.dto.ResultStage;
import com.arriyiaconsulting.siasaleo.service.domain.result.repository.ResultRepository;
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

/** Provisional and official publications use the same contest results at every election age. */
@ApplicationScoped
public class ResultPublicationService {
    @Inject private ResultRepository repo;
    @Inject private Validator validator;

    public PublicationDto find(long id,boolean admin) {
        PublicationDto p=repo.find(id,false).orElseThrow(RecordNotFoundException::new);
        if(!admin && p.status()!=PublicationStatus.PUBLISHED) throw new RecordNotFoundException();
        return p;
    }
    public List<PublicationDto> publications(Long contest,boolean admin,int page,int size) { return repo.list(contest,admin,offset(page,size),size(size)); }
    /**
     * Everything checkable without the database. Field rules are declared on
     * ResultRequest and its rows; only rules spanning several rows live here.
     */
    public static List<String> shapeErrors(Validator validator,ResultRequest r) {
        if(r==null) return List.of("Result body is required");
        // Before anything else, so an oversized payload is refused without
        // validating every one of its rows.
        if(list(r.candidacyIds()).size()+list(r.votes()).size()+list(r.ballots()).size()>10000) return List.of("Use at most 10000 rows per batch");
        List<String> e=new ArrayList<>(violations(validator,r));
        Set<Long> candidates=new HashSet<>();
        for(Long id:list(r.candidacyIds())) {
            if(id!=null && !candidates.add(id)) e.add("Duplicate candidacy: "+id);
        }
        Set<String> votes=new HashSet<>();
        for(VoteRow v:list(r.votes())) {
            if(v!=null && v.candidacyId()!=null && v.areaId()!=null && !votes.add(v.candidacyId()+":"+v.areaId()))
                e.add("Duplicate vote for candidacy "+v.candidacyId()+" in area "+v.areaId());
        }
        Set<Long> ballotAreas=new HashSet<>();
        for(BallotRow b:list(r.ballots())) {
            if(b==null || b.areaId()==null) continue;
            if(!ballotAreas.add(b.areaId())) e.add("Duplicate ballot area: "+b.areaId());
            if(b.rejectedBallots()!=null && b.ballotsCast()!=null && b.rejectedBallots()>b.ballotsCast())
                e.add("Rejected ballots exceed ballots cast in area "+b.areaId());
        }
        return e;
    }
    public ValidationReport preview(ResultRequest r) { return preview(r,null); }
    public ValidationReport previewBatch(long id,ResultRequest r) { requireContext(find(id,true),r); return preview(r,id); }
    private ValidationReport preview(ResultRequest r,Long existing) {
        List<String> e=new ArrayList<>(shapeErrors(validator,r));
        if(!e.isEmpty()) return new ValidationReport(false,e);
        var contest=repo.rows("SELECT geography_snapshot_id,jurisdiction_id FROM contest WHERE id=?1",r.contestId());
        if(contest.isEmpty() || contest.getFirst()[0]==null || contest.getFirst()[1]==null) return new ValidationReport(false,List.of("Contest requires its election geography and jurisdiction"));
        Long geography=number(contest.getFirst()[0]),scope=number(contest.getFirst()[1]);
        if(!repo.exists("SELECT count(*) FROM electoral_area_snapshot WHERE snapshot_id=?1 AND area_type_id=?2 AND area_in_snapshot_scope(id,?3)",geography,r.areaTypeId(),scope)) e.add("No areas at requested granularity in contest scope");
        Set<Long> candidates=new HashSet<>(list(r.candidacyIds())), areas=new HashSet<>();
        for(Long candidate:candidates) if(!repo.exists("SELECT count(*) FROM candidacy WHERE id=?1 AND contest_id=?2 AND ballot_name IS NOT NULL AND btrim(ballot_name)<>'' AND ((political_party_id IS NULL AND ballot_party_name IS NULL) OR (political_party_id IS NOT NULL AND ballot_party_name IS NOT NULL AND btrim(ballot_party_name)<>''))",candidate,r.contestId())) e.add("Candidacy must belong to the contest and have recorded ballot labels: "+candidate);
        for(VoteRow v:list(r.votes())) {
            areas.add(v.areaId());
            if(!candidates.contains(v.candidacyId()) && (existing==null || !repo.exists("SELECT count(*) FROM result_candidate WHERE publication_id=?1 AND candidacy_id=?2",existing,v.candidacyId()))) e.add("Vote references a candidacy outside the publication roster: "+v.candidacyId());
        }
        list(r.ballots()).forEach(b->areas.add(b.areaId()));
        for(Long area:areas) if(!repo.exists("SELECT count(*) FROM electoral_area_snapshot WHERE id=?1 AND snapshot_id=?2 AND area_type_id=?3 AND area_in_snapshot_scope(id,?4)",area,geography,r.areaTypeId(),scope)) e.add("Area outside contest geography, granularity or scope: "+area);
        if(r.supersedesId()!=null) {
            PublicationDto p=repo.find(r.supersedesId(),false).orElse(null);
            if(p==null || p.status()!=PublicationStatus.PUBLISHED || !Objects.equals(p.contestId(),r.contestId()) || !p.stage().name().equals(r.stage())) e.add("Predecessor must be published for the same contest and stage");
        }
        return new ValidationReport(e.isEmpty(),e);
    }
    @Transactional
    public ResultImportResult create(ResultRequest r) {
        requireValid(preview(r));
        String digest=fingerprint(r);
        repo.scalar("SELECT 1 FROM pg_advisory_xact_lock(cast(hashtext(?1) as bigint))","results:"+r.contestId()+":"+r.stage()+":"+r.sourceSha256());
        var existing=repo.rows("SELECT id,import_fingerprint FROM result_publication WHERE contest_id=?1 AND stage=?2 AND area_type_id=?3 AND source_sha256=?4",r.contestId(),r.stage(),r.areaTypeId(),r.sourceSha256());
        if(!existing.isEmpty()) {
            if(!digest.equals(existing.getFirst()[1])) throw new RecordConflictException("Source already has a different result payload; create a sourced revision");
            return result(number(existing.getFirst()[0]),true);
        }
        long id=repo.insert("INSERT INTO result_publication(contest_id,geography_snapshot_id,scope_area_id,area_type_id,stage,source_reference,source_sha256,coverage,supersedes_id,import_fingerprint) SELECT id,geography_snapshot_id,jurisdiction_id,?2,?3,?4,?5,?6,?7,?8 FROM contest WHERE id=?1 RETURNING id",r.contestId(),r.areaTypeId(),r.stage(),r.sourceReference(),r.sourceSha256(),r.coverage(),r.supersedesId(),digest);
        insertBatch(id,r,digest);
        return result(id,false);
    }
    @Transactional
    public ResultImportResult append(long id,ResultRequest r) {
        PublicationDto p=repo.find(id,true).orElseThrow(RecordNotFoundException::new);
        requireContext(p,r); String digest=fingerprint(r);
        if(repo.exists("SELECT count(*) FROM result_import_batch WHERE publication_id=?1 AND fingerprint=?2",id,digest)) return result(id,true);
        if(p.status()!=PublicationStatus.DRAFT) throw new RecordConflictException("Published results require a revision");
        requireValid(preview(r,id)); insertBatch(id,r,digest); return result(id,false);
    }
    private void insertBatch(long id,ResultRequest r,String digest) {
        for(Long candidate:list(r.candidacyIds())) repo.execute("INSERT INTO result_candidate(publication_id,candidacy_id,candidate_name,party_name,independent) SELECT ?1,id,ballot_name,ballot_party_name,political_party_id IS NULL FROM candidacy WHERE id=?2",id,candidate);
        for(VoteRow v:list(r.votes())) repo.execute("INSERT INTO contest_result(publication_id,candidacy_id,area_id,vote_count,stage,source_record_reference) VALUES(?1,?2,?3,?4,?5,?6)",id,v.candidacyId(),v.areaId(),v.voteCount(),r.stage(),v.sourceRecordReference());
        for(BallotRow b:list(r.ballots())) repo.execute("INSERT INTO result_ballot(publication_id,area_id,ballots_cast,rejected_ballots,source_record_reference) VALUES(?1,?2,?3,?4,?5)",id,b.areaId(),b.ballotsCast(),b.rejectedBallots(),b.sourceRecordReference());
        repo.execute("INSERT INTO result_import_batch(publication_id,fingerprint) VALUES(?1,?2)",id,digest);
    }
    public ValidationReport validation(long id) { find(id,true); var e=repo.errors(id); return new ValidationReport(e.isEmpty(),e); }
    @Transactional
    public PublicationDto publish(long id) {
        PublicationDto p=repo.find(id,true).orElseThrow(RecordNotFoundException::new);
        if(p.status()==PublicationStatus.PUBLISHED) return p;
        requireValid(validation(id));
        repo.execute("UPDATE result_publication SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP WHERE id=?1",id);
        return find(id,true);
    }
    @Transactional
    public void deleteDraft(long id) {
        if(repo.find(id,true).orElseThrow(RecordNotFoundException::new).status()!=PublicationStatus.DRAFT) throw new RecordConflictException("Published results are immutable");
        for(String table:List.of("result_import_batch","result_ballot","contest_result","result_candidate")) repo.execute("DELETE FROM "+table+" WHERE publication_id=?1",id);
        repo.execute("DELETE FROM result_publication WHERE id=?1",id);
    }
    private ResultImportResult result(long id,boolean replay) { return new ResultImportResult(find(id,true),replay,validation(id)); }
    private void requireContext(PublicationDto p,ResultRequest r) {
        if(r==null || !Objects.equals(p.contestId(),r.contestId()) || !Objects.equals(p.areaTypeId(),r.areaTypeId()) || !p.stage().name().equals(r.stage()) || !Objects.equals(p.sourceReference(),r.sourceReference()) || !Objects.equals(p.sourceSha256(),r.sourceSha256()) || !p.coverage().name().equals(r.coverage()) || !Objects.equals(p.supersedesId(),r.supersedesId())) throw new IllegalArgumentException("Batch metadata must match its result publication");
    }
}
