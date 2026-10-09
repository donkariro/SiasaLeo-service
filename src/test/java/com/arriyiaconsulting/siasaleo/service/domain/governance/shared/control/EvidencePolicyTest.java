package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control.DigitalAssetService;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.DigitalAssetDto;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordNotFoundException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class EvidencePolicyTest {
 @Mock DigitalAssetService assets;
 @InjectMocks EvidencePolicy policy;
 @Test void publicSealedSourceWithProvenanceIsAccepted() {
  var v=version("County archive"); when(assets.asset(4,null)).thenReturn(asset(null));
  assertSame(v,policy.requireEvidence(8));
 }
 @Test void missingProvenanceIsRejected() {
  version(null); when(assets.asset(4,null)).thenReturn(asset(null));
  assertThrows(IllegalArgumentException.class,()->policy.requireEvidence(8));
 }
 @Test void blankProvenanceIsRejected() {
  version("  "); when(assets.asset(4,null)).thenReturn(asset(null));
  assertThrows(IllegalArgumentException.class,()->policy.requireEvidence(8));
 }
 @Test void archivedSourceCannotBeAttached() {
  version("Archive"); when(assets.asset(4,null)).thenReturn(asset(OffsetDateTime.now()));
  assertThrows(IllegalArgumentException.class,()->policy.requireEvidence(8));
 }
 @Test void privateSourceIsNotAutomaticallyPublished() {
  version("Archive"); when(assets.asset(4,null)).thenThrow(new RecordNotFoundException());
  assertThrows(RecordNotFoundException.class,()->policy.requireEvidence(8));
  verify(assets,never()).visibility(anyLong(),any(),anyLong());
 }
 @Test void draftIsRejectedBySharedFinalizationRule() {
  when(assets.requireSealed(8)).thenThrow(new IllegalArgumentException("Unsealed"));
  assertThrows(IllegalArgumentException.class,()->policy.requireEvidence(8)); verify(assets,never()).asset(anyLong(),any());
 }
 private DigitalAssetVersion version(String provenance) {
  var v=new DigitalAssetVersion(); v.setAssetId(4L); v.setProvenance(provenance);
  when(assets.requireSealed(8)).thenReturn(v); return v;
 }
 private DigitalAssetDto asset(OffsetDateTime archived) {
  return new DigitalAssetDto(4L,"Report",null,"REPORT",AssetVisibility.PUBLIC,1L,null,1L,null,archived,0L);
 }
}
