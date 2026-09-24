package com.arriyiaconsulting.siasaleo.service.domain.result.control;
import com.arriyiaconsulting.siasaleo.service.domain.result.dto.ResultDtos.*;
import com.arriyiaconsulting.siasaleo.service.domain.result.dto.ResultStage;
import com.arriyiaconsulting.siasaleo.service.domain.result.repository.ResultRepository;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.validation.TestValidation;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.Coverage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.PublicationStatus;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultPublicationServiceTest {
    @Mock ResultRepository repo;
    @InjectMocks ResultPublicationService service;
    private PublicationDto publication(PublicationStatus status) { return new PublicationDto(1L,2L,3L,4L,7L,ResultStage.OFFICIAL,"source","a".repeat(64),Coverage.COMPLETE,null,status,"now",null); }
    @Test void publicReadsHideDrafts() {
        when(repo.find(1,false)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        assertThrows(RecordNotFoundException.class,()->service.find(1,false));
        assertEquals(PublicationStatus.DRAFT,service.find(1,true).status());
    }
    @Test void incompletePublicationCannotPublish() {
        when(repo.find(1,true)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        when(repo.find(1,false)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        when(repo.errors(1)).thenReturn(List.of("Missing candidate counts"));
        assertThrows(IllegalArgumentException.class,()->service.publish(1));
        verify(repo,never()).execute(anyString(),any(Object[].class));
    }
    @Test void publishedDataCannotBeDeleted() {
        when(repo.find(1,true)).thenReturn(Optional.of(publication(PublicationStatus.PUBLISHED)));
        assertThrows(RecordConflictException.class,()->service.deleteDraft(1));
    }
    @Test void provisionalAndOfficialUseIdenticalInputStructure() {
        for(String stage:List.of("PROVISIONAL","OFFICIAL")) {
            var r=new ResultRequest(2L,7L,stage,"source","a".repeat(64),"PARTIAL",null,List.of(9L),List.of(new VoteRow(9L,10L,0L,null)),null);
            assertTrue(ResultPublicationService.shapeErrors(TestValidation.validator(),r).isEmpty());
        }
    }
    @Test void rejectsMissingCandidacyAndInvalidBallots() {
        var r=new ResultRequest(2L,7L,"OFFICIAL","source","a".repeat(64),"PARTIAL",null,List.of(9L),List.of(new VoteRow(null,10L,1L,null)),List.of(new BallotRow(10L,5L,6L,null)));
        assertEquals(2,ResultPublicationService.shapeErrors(TestValidation.validator(),r).size());
    }
    private static List<String> shapeErrors(ResultRequest r) { return ResultPublicationService.shapeErrors(TestValidation.validator(),r); }
    @Test void unknownStageAndCoverageNameTheAllowedValues() {
        var r=new ResultRequest(2L,7L,"FINAL","source","a".repeat(64),"ALL",null,List.of(9L),List.of(new VoteRow(9L,10L,0L,null)),null);
        assertEquals(List.of("coverage: must be one of COMPLETE, PARTIAL","stage: must be one of OFFICIAL, PROVISIONAL"),shapeErrors(r));
    }
    @Test void errorsNameTheOffendingRowAndField() {
        var r=new ResultRequest(2L,7L,"OFFICIAL","source","a".repeat(64),"PARTIAL",null,List.of(9L),
                List.of(new VoteRow(9L,10L,5L,null),new VoteRow(9L,11L,-1L,null)),null);
        assertEquals(List.of("votes[1].voteCount: must be greater than or equal to 0"),shapeErrors(r));
    }
    // Record components carry their constraints to the field, accessor and
    // constructor; each rule must still be reported once, not per copy.
    @Test void eachMissingFieldIsReportedOnce() {
        var r=new ResultRequest(null,7L,"OFFICIAL","source","a".repeat(64),"PARTIAL",null,List.of(9L),List.of(new VoteRow(9L,10L,0L,null)),null);
        assertEquals(List.of("contestId: must not be null"),shapeErrors(r));
    }
    @Test void crossRowRulesStillApply() {
        var r=new ResultRequest(2L,7L,"OFFICIAL","source","a".repeat(64),"PARTIAL",null,List.of(9L,9L),
                List.of(new VoteRow(9L,10L,1L,null),new VoteRow(9L,10L,2L,null)),List.of(new BallotRow(10L,5L,6L,null)));
        assertEquals(List.of("Duplicate candidacy: 9","Duplicate vote for candidacy 9 in area 10","Rejected ballots exceed ballots cast in area 10"),shapeErrors(r));
    }
    @Test void oversizedBatchIsRefusedBeforeRowValidation() {
        var rows=Collections.nCopies(10001,new VoteRow(null,null,-1L,null));
        var r=new ResultRequest(2L,7L,"OFFICIAL","source","a".repeat(64),"PARTIAL",null,List.of(9L),rows,null);
        assertEquals(List.of("Use at most 10000 rows per batch"),shapeErrors(r));
    }
    // Stored stage/coverage are enums while request fields are text; a batch
    // whose metadata matches must be recognised as matching, not rejected.
    private ResultRequest batch(String stage,String coverage) {
        return new ResultRequest(2L,7L,stage,"source","a".repeat(64),coverage,null,List.of(9L),List.of(new VoteRow(9L,10L,0L,null)),null);
    }
    @Test void batchWithMatchingMetadataIsAccepted() {
        when(repo.find(1,true)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        when(repo.find(1,false)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        when(repo.exists(anyString(),any(Object[].class))).thenReturn(true);
        assertTrue(service.append(1,batch("OFFICIAL","COMPLETE")).replayed());
    }
    @Test void batchWithDifferentStageOrCoverageIsRejected() {
        when(repo.find(1,true)).thenReturn(Optional.of(publication(PublicationStatus.DRAFT)));
        assertThrows(IllegalArgumentException.class,()->service.append(1,batch("PROVISIONAL","COMPLETE")));
        assertThrows(IllegalArgumentException.class,()->service.append(1,batch("OFFICIAL","PARTIAL")));
    }
}
