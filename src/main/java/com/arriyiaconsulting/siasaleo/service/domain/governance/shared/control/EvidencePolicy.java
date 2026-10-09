package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control.DigitalAssetService;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
/** Governance eligibility is a consumer policy, separate from media finalization. */
@ApplicationScoped
public class EvidencePolicy {
 @Inject private DigitalAssetService assets;
 public DigitalAssetVersion requireEvidence(long id) {
  DigitalAssetVersion version=assets.requireSealed(id);
  var asset=assets.asset(version.getAssetId(),null);
  if(asset.archivedAt()!=null || asset.visibility()!=AssetVisibility.PUBLIC || version.getProvenance()==null || version.getProvenance().isBlank())
   throw new IllegalArgumentException("Evidence requires an active public asset with provenance");
  return version;
 }
}
