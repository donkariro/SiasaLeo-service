package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface DigitalAssetTypeRepository extends BasicRepository<DigitalAssetType, String> {
    @Insert DigitalAssetType insert(DigitalAssetType entity);
    @Update DigitalAssetType update(DigitalAssetType entity);
    @Query("FROM DigitalAssetType ORDER BY code")
    List<DigitalAssetType> ordered();
}
