package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface DigitalAssetRepository extends BasicRepository<DigitalAsset, Long> {
    @Insert DigitalAsset insert(DigitalAsset entity);
    @Update DigitalAsset update(DigitalAsset entity);
    @Query("FROM DigitalAsset WHERE id IN :ids ORDER BY id DESC")
    List<DigitalAsset> findByIds(@Param("ids") List<Long> ids);
    @Query("FROM DigitalAsset ORDER BY id")
    List<DigitalAsset> ordered();
}
