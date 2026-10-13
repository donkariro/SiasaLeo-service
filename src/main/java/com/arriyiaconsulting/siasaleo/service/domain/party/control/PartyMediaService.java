package com.arriyiaconsulting.siasaleo.service.domain.party.control;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control.DigitalAssetService;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.DigitalAssetDetailDto;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository.AssetManagementRepository;
import com.arriyiaconsulting.siasaleo.service.domain.party.dto.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.ArrayList;

@ApplicationScoped
public class PartyMediaService {
 @Inject private DigitalAssetService assets;
 @Inject private AssetManagementRepository access;
 @Inject private NativeQueries sql;
 private void purpose(String purpose) {
  if(purpose==null || purpose.isBlank() || purpose.length()>100) throw new IllegalArgumentException("A purpose of 1 to 100 characters is required");
 }
 @Transactional public DigitalAssetDetailDto select(long party,String purpose,long asset,long actor) {
  purpose(purpose); assets.requirePartyManager(party,actor);
  var detail=assets.link(asset,actor);
  sql.execute("""
   INSERT INTO party_media_selection(party_id,purpose,asset_id,selected_by_account_id)
   VALUES(?1,?2,?3,?4) ON CONFLICT(party_id,purpose) DO UPDATE
   SET asset_id=EXCLUDED.asset_id,selected_by_account_id=EXCLUDED.selected_by_account_id,selected_at=CURRENT_TIMESTAMP
   """,party,purpose,asset,actor);
  return detail;
 }
 public DigitalAssetDetailDto current(long party,String purpose,Long actor) {
  purpose(purpose);
  var rows=sql.rows("SELECT asset_id,asset_id FROM party_media_selection WHERE party_id=?1 AND purpose=?2",party,purpose);
  if(rows.isEmpty()) throw new RecordNotFoundException();
  return assets.current(((Number)rows.getFirst()[0]).longValue(),actor);
 }
 @Transactional public void remove(long party,String purpose,long actor) {
  purpose(purpose); assets.requirePartyManager(party,actor);
  sql.execute("DELETE FROM party_media_selection WHERE party_id=?1 AND purpose=?2",party,purpose);
 }
 @Transactional public void manager(long party,long account,long actor,boolean remove) {
  if(!access.partyExists(party)) throw new RecordNotFoundException();
  if(!remove && !access.activeAccount(account)) throw new IllegalArgumentException("An active account is required");
  access.actor(actor); access.partyManager(party,account,actor,remove);
 }
 @Transactional public PersonDigitalAssetDto attach(long person,String purpose,MediaAttachmentRequest request,long actor) {
  purpose(purpose); assets.requirePartyManager(person,actor);
  if(!sql.exists("SELECT count(*) FROM person WHERE id=?1",person)) throw new RecordNotFoundException();
  var version=assets.requireSealed(request.assetVersionId());
  assets.link(version.getAssetId(),actor);
  assets.read(request.assetVersionId(),actor);
  sql.execute("""
   INSERT INTO person_digital_asset(person_id,asset_version_id,purpose,caption) VALUES(?1,?2,?3,?4)
   ON CONFLICT(person_id,asset_version_id,purpose) DO UPDATE SET caption=EXCLUDED.caption
   """,person,request.assetVersionId(),purpose,request.caption());
  return new PersonDigitalAssetDto(person,request.assetVersionId(),purpose,request.caption());
 }
 public List<PersonDigitalAssetDto> attachments(long person,Long actor,int page,int size) {
  int offset=DigitalAssetService.offset(page,size);
  // Filter before pagination: neither private associations nor captions leak.
  var rows=sql.rows("""
   SELECT p.asset_version_id,p.purpose,p.caption FROM person_digital_asset p
   JOIN digital_asset_version v ON v.id=p.asset_version_id JOIN digital_asset a ON a.id=v.asset_id
   WHERE p.person_id=?1 AND (a.visibility='PUBLIC' OR a.owner_account_id=?2
    OR EXISTS(SELECT 1 FROM digital_asset_grant g WHERE g.asset_id=a.id AND g.account_id=?2)
    OR EXISTS(SELECT 1 FROM party_media_manager m WHERE m.party_id=a.owner_party_id AND m.account_id=?2)
    OR EXISTS(SELECT 1 FROM user_account u WHERE u.id=?2 AND u.person_id=a.owner_party_id AND u.status='ACTIVE'))
   ORDER BY p.asset_version_id,p.purpose LIMIT ?3 OFFSET ?4
   """,person,actor,size,offset);
  return rows.stream().map(r->new PersonDigitalAssetDto(person,((Number)r[0]).longValue(),(String)r[1],(String)r[2])).toList();
 }
 @Transactional public void detach(long person,long version,String purpose,long actor) {
  assets.requirePartyManager(person,actor);
  sql.execute("DELETE FROM person_digital_asset WHERE person_id=?1 AND asset_version_id=?2 AND purpose=?3",person,version,purpose);
 }
}
