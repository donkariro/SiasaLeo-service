package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.mapping.DigitalAssetMapper;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.data.Limit;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigitalAssetServiceTest {
 @Spy private DigitalAssetMapper mapper=Mappers.getMapper(DigitalAssetMapper.class);
 @Mock private DigitalAssetRepository assets;
 @Mock private DigitalAssetVersionRepository versions;
 @Mock private StoredFileRepository files;
 @Mock private DigitalAssetTypeRepository types;
 @Mock private AssetManagementRepository management;
 @InjectMocks private DigitalAssetService service;

 @Test void creationDefaultsToPrivateAndSeparatesCreatorFromOwner() {
  activeType(); when(assets.insert(any())).then(returnsFirstArg());
  var result=service.create(new CreateDigitalAssetRequest("Report",null,"REPORT",null),1);
  assertEquals(AssetVisibility.PRIVATE,result.visibility());
  assertEquals(1L,result.createdByAccountId()); assertEquals(1L,result.ownerAccountId());
  verify(management).actor(1);
 }
 @Test void partyOwnershipRequiresAuthority() {
  activeType();
  assertThrows(RecordAccessDeniedException.class,()->service.create(new CreateDigitalAssetRequest("Logo",null,"REPORT",null,9L),1));
  verify(assets,never()).insert(any());
 }
 @Test void partyManagerCanCreateForParty() {
  activeType(); when(management.partyManager(9,1)).thenReturn(true); when(assets.insert(any())).then(returnsFirstArg());
  var result=service.create(new CreateDigitalAssetRequest("Logo",null,"REPORT",null,9L),1);
  assertEquals(9L,result.ownerPartyId()); assertNull(result.ownerAccountId()); assertEquals(1L,result.createdByAccountId());
 }
 @Test void retiredTypesAreRejected() {
  DigitalAssetType type=new DigitalAssetType(); type.setActive(false);
  when(types.findById("REPORT")).thenReturn(Optional.of(type));
  assertThrows(IllegalArgumentException.class,()->service.create(new CreateDigitalAssetRequest("Report",null,"REPORT",null),1));
 }
 @Test void sealedPrivateVersionIsHiddenFromPublic() {
  version(true); asset(AssetVisibility.PRIVATE);
  assertThrows(RecordNotFoundException.class,()->service.read(8,null));
  verifyNoInteractions(files);
 }
 @Test void ownerCanReadPrivateDraft() {
  version(false); asset(AssetVisibility.PRIVATE);
  assertNotNull(service.read(8,1L));
 }
 @Test void readerCanReadPrivateSealedVersionButNotDraft() {
  var v=version(true); asset(AssetVisibility.PRIVATE);
  when(management.hasGrant(4,2,false)).thenReturn(true);
  assertNotNull(service.read(8,2L));
  v.setSealedAt(null);
  assertThrows(RecordNotFoundException.class,()->service.read(8,2L));
 }
 @Test void managerCanReadDraft() {
  version(false); asset(AssetVisibility.PRIVATE);
  when(management.hasGrant(4,2,false)).thenReturn(true); when(management.hasGrant(4,2,true)).thenReturn(true);
  assertNotNull(service.read(8,2L));
 }
 @Test void publicDraftIsStillHidden() {
  version(false); asset(AssetVisibility.PUBLIC);
  assertThrows(RecordNotFoundException.class,()->service.read(8,null));
 }
 @Test void publicSealedVersionIsReadable() {
  version(true); asset(AssetVisibility.PUBLIC);
  assertNotNull(service.read(8,null));
 }
 @Test void strangerCannotSeal() {
  var a=locked(AssetVisibility.PRIVATE); version(false);
  assertThrows(RecordAccessDeniedException.class,()->service.seal(8,2));
  verify(versions,never()).update(any());
 }
 @Test void sealingSnapshotsDescriptionWithoutPublishing() {
  var a=locked(AssetVisibility.PRIVATE); when(assets.findById(4L)).thenReturn(Optional.of(a));
  a.setTitle("Original title"); a.setDescription("Original description"); a.setAssetType("REPORT");
  var v=version(false); v.setSourceUrl("https://example.test/report");
  var result=service.seal(8,1);
  assertEquals("Original title",result.version().snapshotTitle());
  assertEquals("Original description",result.version().snapshotDescription());
  assertEquals(AssetVisibility.PRIVATE,result.asset().visibility());
 }
 @Test void archivedAssetsRejectNewVersions() {
  var a=locked(AssetVisibility.PRIVATE); a.setArchivedAt(OffsetDateTime.now());
  assertThrows(IllegalArgumentException.class,()->service.version(4,new CreateDigitalAssetVersionRequest(null,null,null,null,null),1));
  verify(versions,never()).insert(any());
 }
 @Test void versionAllocationLocksAssetBeforeReadingCounter() {
  locked(AssetVisibility.PRIVATE); when(versions.latestNumber(4L)).thenReturn(3);
  when(versions.insert(any())).then(returnsFirstArg());
  assertEquals(4,service.version(4,new CreateDigitalAssetVersionRequest(null,null,null,null,null),1).versionNumber());
  InOrder order=inOrder(management,versions); order.verify(management).lock(4); order.verify(management).actor(1); order.verify(versions).latestNumber(4L);
 }
 @Test void managerCannotGrantThemselvesOwnershipPowers() {
  locked(AssetVisibility.PRIVATE);
  assertThrows(RecordAccessDeniedException.class,()->service.grant(4,3,AssetPermission.MANAGE,2));
  verify(management,never()).grant(anyLong(),anyLong(),any());
 }
 @Test void ownerCanShareAndRevoke() {
  locked(AssetVisibility.PRIVATE); when(management.activeAccount(2)).thenReturn(true);
  service.grant(4,2,AssetPermission.READ,1); service.revoke(4,2,1);
  verify(management).grant(4,2,AssetPermission.READ); verify(management).revoke(4,2);
 }
 @Test void visibilityChangeRequiresOwner() {
  locked(AssetVisibility.PRIVATE);
  assertThrows(RecordAccessDeniedException.class,()->service.visibility(4,AssetVisibility.PUBLIC,2));
 }
 @Test void staleMetadataUpdateIsRejected() {
  locked(AssetVisibility.PRIVATE);
  assertThrows(RecordConflictException.class,()->service.update(4,new UpdateAssetRequest("Changed",null,"REPORT",99L),1));
  verify(assets,never()).update(any());
 }
 @Test void ownershipTransferClearsGrantsButPreservesCreator() {
  var a=locked(AssetVisibility.PRIVATE); when(management.activeAccount(2)).thenReturn(true); when(management.update(any())).then(returnsFirstArg());
  var result=service.transfer(4,new AssetOwnerRequest(2L,null),1,false);
  assertEquals(2L,result.ownerAccountId()); assertEquals(1L,result.createdByAccountId()); verify(management).clearGrants(4);
 }
 @Test void administratorCannotTakeAnOwnedAsset() {
  locked(AssetVisibility.PRIVATE);
  assertThrows(RecordAccessDeniedException.class,()->service.transfer(4,new AssetOwnerRequest(2L,null),2,true));
 }
 @Test void sealedVersionCannotBeDeleted() {
  locked(AssetVisibility.PRIVATE); version(true);
  assertThrows(IllegalArgumentException.class,()->service.deleteDraft(8,1)); verify(management,never()).deleteDraft(anyLong());
 }
 @Test void privateCurrentIsHidden() {
  asset(AssetVisibility.PRIVATE);
  assertThrows(RecordNotFoundException.class,()->service.current(4,null));
  verify(versions,never()).sealedNewestFirst(any(),any());
 }
 @Test void currentUsesLatestSealedVersion() {
  asset(AssetVisibility.PUBLIC);
  var v=new DigitalAssetVersion(); id(v,8L); v.setVersionNumber(3);
  when(versions.sealedNewestFirst(eq(4L),any(Limit.class))).thenReturn(List.of(v));
  assertEquals(3,service.current(4,null).version().versionNumber());
 }
 @Test void generalSealedVersionsDoNotRequireProvenance() {
  var v=version(true); v.setProvenance(null); assertSame(v,service.requireSealed(8));
 }
 @Test void invalidPaginationIsRejected() {
  assertThrows(IllegalArgumentException.class,()->service.list(null,null,null,false,-1,25));
  assertThrows(IllegalArgumentException.class,()->service.history(4,null,0,101));
  assertThrows(IllegalArgumentException.class,()->DigitalAssetService.offset(Integer.MAX_VALUE,100));
 }
 @Test void storageKeysAreNotPublic() {
  assertTrue(Arrays.stream(StoredFileDto.class.getRecordComponents()).noneMatch(c->c.getName().equals("storageKey")));
 }
 private void activeType() { DigitalAssetType t=new DigitalAssetType(); t.setCode("REPORT"); when(types.findById("REPORT")).thenReturn(Optional.of(t)); }
 private DigitalAsset makeAsset(AssetVisibility visibility) {
  DigitalAsset a=new DigitalAsset(); id(a,4L); a.setCreatedByAccountId(1L); a.setOwnerAccountId(1L); a.setVisibility(visibility); return a;
 }
 private DigitalAsset asset(AssetVisibility visibility) {
  var a=makeAsset(visibility); when(assets.findById(4L)).thenReturn(Optional.of(a)); return a;
 }
 private DigitalAsset locked(AssetVisibility visibility) {
  var a=makeAsset(visibility); when(management.lock(4)).thenReturn(Optional.of(a)); return a;
 }
 private DigitalAssetVersion version(boolean sealed) {
  DigitalAssetVersion v=new DigitalAssetVersion(); id(v,8L); v.setAssetId(4L); v.setSealedAt(sealed?OffsetDateTime.now():null);
  when(versions.findById(8L)).thenReturn(Optional.of(v)); return v;
 }
 private static void id(Object object,Long id) {
  try { var field=object.getClass().getDeclaredField("id"); field.setAccessible(true); field.set(object,id); }
  catch(ReflectiveOperationException e) { throw new AssertionError(e); }
 }
}
