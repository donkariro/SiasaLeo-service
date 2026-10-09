package com.arriyiaconsulting.siasaleo.service.domain.party.control;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control.DigitalAssetService;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository.AssetManagementRepository;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.DigitalAssetDetailDto;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class PartyMediaServiceTest {
 @Mock DigitalAssetService assets;
 @Mock AssetManagementRepository access;
 @Mock NativeQueries sql;
 @InjectMocks PartyMediaService service;
 @Test void selectingRequiresAuthorityOverTheProfileAndAccessToTheAsset() {
  doThrow(new RecordAccessDeniedException()).when(assets).requirePartyManager(9,2);
  assertThrows(RecordAccessDeniedException.class,()->service.select(9,"PORTRAIT",4,2));
  verifyNoInteractions(sql); verify(assets,never()).link(anyLong(),anyLong());
 }
 @Test void owningProfileDoesNotAllowSelectingAnotherPrivateAsset() {
  when(assets.link(4,2)).thenThrow(new RecordNotFoundException());
  assertThrows(RecordNotFoundException.class,()->service.select(9,"PORTRAIT",4,2)); verifyNoInteractions(sql);
 }
 @Test void currentSelectionRechecksVisibilityEveryRead() {
  when(sql.rows(anyString(),eq(9L),eq("PORTRAIT"))).thenReturn(List.<Object[]>of(new Object[]{4L,4L}));
  when(assets.current(4,null)).thenThrow(new RecordNotFoundException());
  assertThrows(RecordNotFoundException.class,()->service.current(9,"PORTRAIT",null));
 }
 @Test void successfulSelectionDoesNotPublishTheAsset() {
  var detail=new DigitalAssetDetailDto(null,null,List.of());
  when(assets.link(4,2)).thenReturn(detail);
  assertSame(detail,service.select(9,"PORTRAIT",4,2));
  verify(assets,never()).visibility(anyLong(),any(),anyLong());
  verify(sql).execute(anyString(),eq(9L),eq("PORTRAIT"),eq(4L),eq(2L));
 }
 @Test void removingSelectionDoesNotDeleteItsAsset() {
  service.remove(9,"PORTRAIT",2);
  verify(assets).requirePartyManager(9,2);
  verify(assets,never()).deleteEmpty(anyLong(),anyLong());
 }
}
