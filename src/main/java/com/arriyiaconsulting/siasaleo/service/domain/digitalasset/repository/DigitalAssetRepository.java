package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface DigitalAssetRepository extends BasicRepository<DigitalAsset, Long> {
    @Insert DigitalAsset insert(DigitalAsset entity);
    @Update DigitalAsset update(DigitalAsset entity);
    @Query("FROM DigitalAsset ORDER BY id")
    List<DigitalAsset> ordered();
}
