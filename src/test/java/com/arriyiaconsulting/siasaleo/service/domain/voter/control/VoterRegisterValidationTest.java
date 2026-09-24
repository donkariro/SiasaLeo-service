package com.arriyiaconsulting.siasaleo.service.domain.voter.control;
import com.arriyiaconsulting.siasaleo.service.domain.voter.dto.VoterRegisterDtos.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing.ImportSupport;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.validation.TestValidation;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VoterRegisterValidationTest {
    private RegisterRequest request(List<RegisterCount> counts) { return new RegisterRequest(1L,2L,7L,"source","a".repeat(64),"COMPLETE",null,counts); }
    @Test void zeroIsDifferentFromUnknownAndNegativeCounts() {
        assertTrue(VoterRegisterService.shapeErrors(TestValidation.validator(),request(List.of(new RegisterCount(9L,0L,null)))).isEmpty());
        assertFalse(VoterRegisterService.shapeErrors(TestValidation.validator(),request(List.of(new RegisterCount(9L,null,null)))).isEmpty());
        assertFalse(VoterRegisterService.shapeErrors(TestValidation.validator(),request(List.of(new RegisterCount(9L,-1L,null)))).isEmpty());
    }
    @Test void duplicatesAndOversizedBatchesAreRejected() {
        assertFalse(VoterRegisterService.shapeErrors(TestValidation.validator(),request(Collections.nCopies(2,new RegisterCount(9L,100L,null)))).isEmpty());
        assertFalse(VoterRegisterService.shapeErrors(TestValidation.validator(),request(Collections.nCopies(10001,new RegisterCount(9L,100L,null)))).isEmpty());
        assertFalse(VoterRegisterService.shapeErrors(TestValidation.validator(),request(Arrays.asList((RegisterCount)null))).isEmpty());
    }
    @Test void errorsNameTheOffendingRowAndField() {
        var errors=VoterRegisterService.shapeErrors(TestValidation.validator(),request(List.of(new RegisterCount(9L,100L,null),new RegisterCount(10L,-1L,null))));
        assertEquals(List.of("counts[1].registeredVoters: must be greater than or equal to 0"),errors);
    }
    @Test void payloadFingerprintDetectsChangedCount() {
        var r=request(List.of(new RegisterCount(9L,100L,"row")));
        assertEquals(ImportSupport.fingerprint(r),ImportSupport.fingerprint(request(List.of(new RegisterCount(9L,100L,"row")))));
        assertNotEquals(ImportSupport.fingerprint(r),ImportSupport.fingerprint(request(List.of(new RegisterCount(9L,101L,"row")))));
    }
}
