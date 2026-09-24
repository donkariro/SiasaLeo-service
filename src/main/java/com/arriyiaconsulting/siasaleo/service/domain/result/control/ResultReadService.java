package com.arriyiaconsulting.siasaleo.service.domain.result.control;

import com.arriyiaconsulting.siasaleo.service.domain.result.dto.ResultDtos.*;
import com.arriyiaconsulting.siasaleo.service.domain.result.dto.ResultStage;
import com.arriyiaconsulting.siasaleo.service.domain.result.repository.ResultRepository;
import com.arriyiaconsulting.siasaleo.service.domain.election.dto.RegisterAssignmentDtos.AssignmentDto;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.PublicationStatus;
import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.*;
import java.util.*;
import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.SqlValues.*;

@ApplicationScoped
public class ResultReadService {
    @Inject private ResultRepository repo;
    @Inject private ResultPublicationService publications;
    @Inject private com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegisterService registers;
    @Inject private com.arriyiaconsulting.siasaleo.service.domain.election.control.RegisterAssignmentService assignments;

    public List<CandidateRow> candidates(long id,boolean admin,int page,int size) {
        publications.find(id,admin);
        return repo.rows("SELECT candidacy_id,candidate_name,party_name,independent FROM result_candidate WHERE publication_id=?1 ORDER BY candidacy_id LIMIT ?2 OFFSET ?3",
                id,size(size),offset(page,size)).stream()
                .map(r->new CandidateRow(number(r[0]),text(r[1]),text(r[2]),(Boolean)r[3])).toList();
    }
    public List<VoteRow> votes(long id,Long area,boolean admin,int page,int size) {
        PublicationDto p=publications.find(id,admin);
        return repo.rows("SELECT candidacy_id,area_id,vote_count,source_record_reference FROM contest_result WHERE publication_id=?1 AND area_in_snapshot_scope(area_id,?2) ORDER BY area_id,candidacy_id LIMIT ?3 OFFSET ?4",
                id,scope(p,area),size(size),offset(page,size)).stream()
                .map(r->new VoteRow(number(r[0]),number(r[1]),number(r[2]),text(r[3]))).toList();
    }
    public List<BallotRow> ballots(long id,Long area,boolean admin,int page,int size) {
        PublicationDto p=publications.find(id,admin);
        return repo.rows("SELECT area_id,ballots_cast,rejected_ballots,source_record_reference FROM result_ballot WHERE publication_id=?1 AND area_in_snapshot_scope(area_id,?2) ORDER BY area_id LIMIT ?3 OFFSET ?4",
                id,scope(p,area),size(size),offset(page,size)).stream()
                .map(r->new BallotRow(number(r[0]),number(r[1]),number(r[2]),text(r[3]))).toList();
    }
    public ResultSummary contestResults(long contest,String requestedStage,Long publicationId,Long area) {
        ResultStage stage=ResultStage.parse(requestedStage).orElseThrow(()->new IllegalArgumentException("Invalid result stage"));
        long id;
        if(publicationId!=null) id=publicationId;
        else {
            var rows=repo.rows("SELECT p.id,p.contest_id FROM result_publication p WHERE p.contest_id=?1 AND p.stage=?2 AND p.status='PUBLISHED' "
                    + "AND NOT EXISTS (SELECT 1 FROM result_publication n WHERE n.supersedes_id=p.id AND n.status='PUBLISHED')",contest,stage.name());
            if(rows.isEmpty()) throw new RecordNotFoundException();
            id=number(rows.getFirst()[0]);
        }
        PublicationDto p=publications.find(id,false);
        if(!Objects.equals(p.contestId(),contest) || p.stage()!=stage) throw new RecordNotFoundException();
        return results(id,area,false);
    }
    public ResultSummary results(long id,Long area,boolean admin) {
        PublicationDto p=publications.find(id,admin);
        long scope=scope(p,area), expected=expected(p,scope);
        long actual=repo.count("SELECT count(DISTINCT area_id) FROM contest_result WHERE publication_id=?1 AND area_in_snapshot_scope(area_id,?2)",id,scope);
        List<CandidateTotal> totals=repo.rows("SELECT c.candidacy_id,c.candidate_name,c.party_name,c.independent,sum(v.vote_count),"
                + "round(100.0*sum(v.vote_count)/nullif(sum(sum(v.vote_count)) OVER (),0),2),rank() OVER (ORDER BY sum(v.vote_count) DESC) "
                + "FROM result_candidate c JOIN contest_result v ON v.publication_id=c.publication_id AND v.candidacy_id=c.candidacy_id "
                + "WHERE c.publication_id=?1 AND area_in_snapshot_scope(v.area_id,?2) GROUP BY c.candidacy_id,c.candidate_name,c.party_name,c.independent "
                + "ORDER BY sum(v.vote_count) DESC,c.candidacy_id",id,scope).stream()
                .map(r->new CandidateTotal(number(r[0]),text(r[1]),text(r[2]),(Boolean)r[3],decimal(r[4]),decimal(r[5]),number(r[6]))).toList();
        Object[] ballots=repo.rows("SELECT sum(ballots_cast),CASE WHEN count(rejected_ballots)=count(*) THEN sum(rejected_ballots) END,count(*) FROM result_ballot WHERE publication_id=?1 AND area_in_snapshot_scope(area_id,?2)",id,scope).getFirst();
        long event=((Number)repo.scalar("SELECT election_event_id FROM contest WHERE id=?1",p.contestId())).longValue();
        AssignmentDto assignment=assignments.current(event).orElse(null);
        BigDecimal registered=null,turnout=null;
        String unavailable=null;
        if(expected==0 || actual!=expected || number(ballots[2])!=expected) unavailable="Complete results and ballot totals are required for this area";
        if(assignment==null) unavailable="No official register is assigned to this election";
        else {
            var register=registers.find(assignment.registerId(),false);
            if(!Boolean.TRUE.equals(repo.scalar("SELECT area_in_snapshot_scope(?1,?2)",scope,register.scopeAreaId()))) unavailable="Assigned register does not cover this area";
            else {
                var summary=registers.summary(register.id(),scope,false);
                registered=summary.registeredVoters();
                if(!summary.complete()) unavailable="Assigned register has incomplete coverage at this area";
                else if(registered==null || registered.signum()==0) unavailable="Registered voter denominator is zero or unknown";
            }
        }
        if(p.status()!=PublicationStatus.PUBLISHED) unavailable="Publish and validate this draft before calculating turnout";
        if(unavailable==null) turnout=decimal(ballots[0]).multiply(BigDecimal.valueOf(100)).divide(registered,2,RoundingMode.HALF_UP);
        return new ResultSummary(p,scope,totals,actual,expected,expected>0 && actual==expected,decimal(ballots[0]),decimal(ballots[1]),assignment,registered,turnout,unavailable);
    }
    private long scope(PublicationDto p,Long requested) {
        long area=requested==null?p.scopeAreaId():requested;
        if(!Boolean.TRUE.equals(repo.scalar("SELECT area_in_snapshot_scope(?1,?2)",area,p.scopeAreaId()))) throw new RecordNotFoundException();
        return area;
    }
    private long expected(PublicationDto p,long scope) { return repo.count("SELECT count(*) FROM electoral_area_snapshot WHERE snapshot_id=?1 AND area_type_id=?2 AND area_in_snapshot_scope(id,?3)",p.geographySnapshotId(),p.areaTypeId(),scope); }
}
