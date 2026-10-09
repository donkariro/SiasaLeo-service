package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;

/** Explicit locking for authorization changes and version allocation. */
@ApplicationScoped
public class AssetManagementRepository {
 @PersistenceContext private EntityManager em;
 @Inject private NativeQueries sql;

 public Optional<DigitalAsset> lock(long id) {
  return Optional.ofNullable(em.find(DigitalAsset.class,id,LockModeType.PESSIMISTIC_WRITE));
 }
 public void actor(long actor) { sql.scalar("SELECT set_config('siasaleo.actor',?1,true)",Long.toString(actor)); }
 public boolean partyManager(long party,long actor) {
  return sql.exists("SELECT count(*) FROM party WHERE id=?1 AND (EXISTS(SELECT 1 FROM user_account WHERE id=?2 AND person_id=?1 AND status='ACTIVE') OR EXISTS(SELECT 1 FROM party_media_manager WHERE party_id=?1 AND account_id=?2))",party,actor);
 }
 public boolean hasGrant(long asset,long actor,boolean manage) {
  return sql.exists("SELECT count(*) FROM digital_asset_grant WHERE asset_id=?1 AND account_id=?2 AND (?3=false OR permission='MANAGE')",asset,actor,manage);
 }
 public boolean activeAccount(long actor) { return sql.exists("SELECT count(*) FROM user_account WHERE id=?1 AND status='ACTIVE'",actor); }
 public boolean partyExists(long party) { return sql.exists("SELECT count(*) FROM party WHERE id=?1",party); }
 public DigitalAsset update(DigitalAsset asset) { em.flush(); return asset; }
 public void grant(long asset,long actor,AssetPermission permission) {
  sql.execute("INSERT INTO digital_asset_grant(asset_id,account_id,permission) VALUES(?1,?2,?3) ON CONFLICT(asset_id,account_id) DO UPDATE SET permission=EXCLUDED.permission",asset,actor,permission.name());
 }
 public void revoke(long asset,long actor) { sql.execute("DELETE FROM digital_asset_grant WHERE asset_id=?1 AND account_id=?2",asset,actor); }
 public void clearGrants(long asset) { sql.execute("DELETE FROM digital_asset_grant WHERE asset_id=?1",asset); }
 public List<AssetGrantDto> grants(long asset) {
  return sql.rows("SELECT account_id,permission FROM digital_asset_grant WHERE asset_id=?1 ORDER BY account_id",asset)
   .stream().map(r->new AssetGrantDto(((Number)r[0]).longValue(),(String)r[1])).toList();
 }
 public void partyManager(long party,long account,long actor,boolean remove) {
  if(remove) sql.execute("DELETE FROM party_media_manager WHERE party_id=?1 AND account_id=?2",party,account);
  else sql.execute("INSERT INTO party_media_manager(party_id,account_id,granted_by_account_id) VALUES(?1,?2,?3) ON CONFLICT DO NOTHING",party,account,actor);
 }
 public List<AssetAuditDto> audit(long asset,int offset,int size) {
  return sql.rows("SELECT id,actor_account_id,action,cast(before_data AS text),cast(after_data AS text),cast(recorded_at AS text) FROM digital_asset_audit WHERE asset_id=?1 ORDER BY id DESC LIMIT ?2 OFFSET ?3",asset,size,offset)
   .stream().map(r->new AssetAuditDto(((Number)r[0]).longValue(),r[1]==null?null:((Number)r[1]).longValue(),(String)r[2],(String)r[3],(String)r[4],(String)r[5])).toList();
 }
 public List<Long> search(Long actor,String term,String type,boolean archived,int offset,int size) {
  return sql.rows("""
   SELECT a.id,a.id FROM digital_asset a WHERE
    (?1=false AND a.archived_at IS NULL OR ?1=true) AND
    (?2='' OR position(lower(?2) in lower(a.title))>0) AND
    (?3='' OR a.asset_type=?3) AND
    ((a.visibility='PUBLIC' AND EXISTS(SELECT 1 FROM digital_asset_version v WHERE v.asset_id=a.id AND v.sealed_at IS NOT NULL))
     OR a.owner_account_id=?4
     OR EXISTS(SELECT 1 FROM digital_asset_grant g WHERE g.asset_id=a.id AND g.account_id=?4)
     OR EXISTS(SELECT 1 FROM user_account u WHERE u.id=?4 AND u.person_id=a.owner_party_id AND u.status='ACTIVE')
     OR EXISTS(SELECT 1 FROM party_media_manager m WHERE m.party_id=a.owner_party_id AND m.account_id=?4))
   ORDER BY a.id DESC LIMIT ?5 OFFSET ?6
   """,archived,term,type,actor,size,offset).stream().map(r->((Number)r[0]).longValue()).toList();
 }
 public List<DigitalAssetVersion> history(long asset,boolean drafts,int offset,int size) {
  return em.createQuery("SELECT v FROM DigitalAssetVersion v WHERE v.assetId=:asset AND (:drafts=true OR v.sealedAt IS NOT NULL) ORDER BY v.versionNumber DESC",DigitalAssetVersion.class)
   .setParameter("asset",asset).setParameter("drafts",drafts).setFirstResult(offset).setMaxResults(size).getResultList();
 }
 public void deleteDraft(long version) {
  sql.execute("DELETE FROM stored_file WHERE asset_version_id=?1",version);
  sql.execute("DELETE FROM digital_asset_version WHERE id=?1",version);
 }
 public void deleteEmpty(long asset) { sql.execute("DELETE FROM digital_asset WHERE id=?1",asset); }
 public void flush() { em.flush(); }
}
