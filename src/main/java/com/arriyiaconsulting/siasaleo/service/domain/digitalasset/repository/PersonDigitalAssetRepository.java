package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface PersonDigitalAssetRepository extends BasicRepository<PersonDigitalAsset, PersonDigitalAssetId> {
    @Insert PersonDigitalAsset insert(PersonDigitalAsset entity);
    @Update PersonDigitalAsset update(PersonDigitalAsset entity);
    @Query("FROM PersonDigitalAsset ORDER BY personId, assetVersionId, purpose")
    List<PersonDigitalAsset> ordered();
}
