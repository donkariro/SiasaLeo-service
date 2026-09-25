package com.arriyiaconsulting.siasaleo.service.domain.election.result.control;

import com.arriyiaconsulting.siasaleo.service.domain.election.result.dto.ResultDtos.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.result.dto.ResultStage;
import com.arriyiaconsulting.siasaleo.service.domain.election.result.repository.ResultRepository;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.voter.control.VoterRegisterService;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegisterDtos.*;
import com.arriyiaconsulting.siasaleo.service.domain.election.control.RegisterAssignmentService;
import com.arriyiaconsulting.siasaleo.service.domain.election.dto.RegisterAssignmentDtos.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.Coverage;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.PublicationStatus;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultReadServiceTest {
    @Mock ResultRepository repo;
    @Mock ResultPublicationService publications;
    @Mock VoterRegisterService registers;
    @Mock RegisterAssignmentService assignments;
    @InjectMocks ResultReadService reads;
    private PublicationDto publication(long id,String kind,Long contest) {
        return new PublicationDto(id,contest,1L,2L,7L,ResultStage.OFFICIAL,"archive","a".repeat(64),Coverage.COMPLETE,null,PublicationStatus.PUBLISHED,"now","now");
    }
    private void setup(long ballotAreas,long registerAreas) {
        when(publications.find(1,false)).thenReturn(publication(1,"RESULTS",3L));
        when(registers.find(2,false)).thenReturn(new RegisterDto(2L,1L,2L,7L,"archive","a".repeat(64),Coverage.COMPLETE,null,PublicationStatus.PUBLISHED,"now","now"));
        when(registers.summary(2,2L,false)).thenReturn(new RegisterSummary(null,2L,new BigDecimal("300"),registerAreas,2L,registerAreas==2));
        when(repo.scalar(startsWith("SELECT area_in_snapshot_scope"),any(Object[].class))).thenReturn(true);
        when(repo.scalar(startsWith("SELECT election_event_id"),any(Object[].class))).thenReturn(5L);
        when(repo.count(anyString(),any(Object[].class))).thenReturn(2L);
        when(repo.rows(anyString(),any(Object[].class))).thenAnswer(invocation->{
            String sql=invocation.getArgument(0);
            if(sql.startsWith("SELECT sum(ballots_cast)")) return Collections.singletonList(new Object[]{new BigDecimal("271"),BigDecimal.ONE,ballotAreas});
            if(sql.startsWith("SELECT sum(registered_voters)")) return Collections.singletonList(new Object[]{new BigDecimal("300"),registerAreas});
            return Collections.singletonList(new Object[]{10L,"Candidate A","Party",false,new BigDecimal("270"),new BigDecimal("100"),1L});
        });
        when(assignments.current(5)).thenReturn(Optional.of(new AssignmentDto(7L,5L,2L,null,"source","now")));
    }
    @Test void turnoutUsesBallotsIncludingRejectedAndAssignedOfficialRegister() {
        setup(2,2);
        ResultSummary result=reads.results(1,null,false);
        assertEquals(new BigDecimal("90.33"),result.turnoutPercentage());
        assertNull(result.turnoutUnavailableReason());
        assertEquals(2L,result.registerAssignment().registerId());
        assertEquals(new BigDecimal("300"),result.registeredVoters());
    }
    @Test void missingBallotAreaPreventsTurnout() {
        setup(1,2);
        ResultSummary result=reads.results(1,null,false);
        assertNull(result.turnoutPercentage());
        assertTrue(result.turnoutUnavailableReason().contains("ballot totals"));
    }
    @Test void incompleteRegisterPreventsTurnout() {
        setup(2,1);
        ResultSummary result=reads.results(1,null,false);
        assertNull(result.turnoutPercentage());
        assertTrue(result.turnoutUnavailableReason().contains("incomplete coverage"));
    }
    @Test void explicitRevisionMustBelongToRequestedContest() {
        when(publications.find(1,false)).thenReturn(publication(1,"RESULTS",3L));
        assertThrows(RecordNotFoundException.class,()->reads.contestResults(4,"OFFICIAL",1L,null));
        verifyNoInteractions(repo);
    }
}
