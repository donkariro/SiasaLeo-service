package com.arriyiaconsulting.siasaleo.service.domain.party.repository;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.PersonDigitalAsset;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.PersonDigitalAssetId;
import jakarta.data.repository.*;
import java.util.List;

@Repository
public interface PersonDigitalAssetRepository extends BasicRepository<PersonDigitalAsset, PersonDigitalAssetId> {
    @Insert PersonDigitalAsset insert(PersonDigitalAsset entity);
    @Update PersonDigitalAsset update(PersonDigitalAsset entity);
    @Query("FROM PersonDigitalAsset ORDER BY personId, assetVersionId, purpose")
    List<PersonDigitalAsset> ordered();
}
