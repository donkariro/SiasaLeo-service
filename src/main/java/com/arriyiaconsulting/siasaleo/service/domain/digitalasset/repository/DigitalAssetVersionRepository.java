package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import jakarta.data.Limit;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface DigitalAssetVersionRepository extends BasicRepository<DigitalAssetVersion, Long> {
    @Insert DigitalAssetVersion insert(DigitalAssetVersion entity);
    @Update DigitalAssetVersion update(DigitalAssetVersion entity);
    @Query("FROM DigitalAssetVersion ORDER BY id")
    List<DigitalAssetVersion> ordered();
    @Query("SELECT MAX(versionNumber) FROM DigitalAssetVersion WHERE assetId = :id")
    Integer latestNumber(@Param("id") Long id);
    // Newest first so callers can take the current sealed version with Limit.of(1).
    @Query("FROM DigitalAssetVersion WHERE assetId = :id AND sealedAt IS NOT NULL ORDER BY versionNumber DESC")
    List<DigitalAssetVersion> sealedNewestFirst(@Param("id") Long id, Limit limit);
}
