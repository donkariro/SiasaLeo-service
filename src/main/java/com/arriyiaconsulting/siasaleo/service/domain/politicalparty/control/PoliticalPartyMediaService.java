package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control.DigitalAssetService;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.DigitalAssetDetailDto;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
@ApplicationScoped
public class PoliticalPartyMediaService {
 @Inject private DigitalAssetService assets;
 @Inject private NativeQueries sql;
 @Transactional public DigitalAssetDetailDto select(long record,long asset,long actor,boolean symbol) {
  String table=symbol?"politicalparty_symbol":"politicalparty_official";
  String column=symbol?"image_asset_id":"photo_asset_id";
  if(!sql.exists("SELECT count(*) FROM "+table+" WHERE id=?1",record)) throw new RecordNotFoundException();
  assets.link(asset,actor);
  var detail=assets.current(asset,null); // These register records are public.
  sql.execute("UPDATE "+table+" SET "+column+"=?1 WHERE id=?2",asset,record);
  return detail;
 }
 public DigitalAssetDetailDto current(long record,boolean symbol) {
  String table=symbol?"politicalparty_symbol":"politicalparty_official";
  String column=symbol?"image_asset_id":"photo_asset_id";
  var rows=sql.rows("SELECT "+column+",id FROM "+table+" WHERE id=?1",record);
  if(rows.isEmpty() || rows.getFirst()[0]==null) throw new RecordNotFoundException();
  return assets.current(((Number)rows.getFirst()[0]).longValue(),null);
 }
}
