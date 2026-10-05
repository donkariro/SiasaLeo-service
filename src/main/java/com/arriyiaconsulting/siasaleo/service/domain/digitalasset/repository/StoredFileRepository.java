package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import jakarta.data.repository.*;
import jakarta.data.page.PageRequest;
import java.util.List;

@Repository
public interface StoredFileRepository extends BasicRepository<StoredFile, Long> {
    @Insert StoredFile insert(StoredFile entity);
    @Update StoredFile update(StoredFile entity);
    @Query("FROM StoredFile ORDER BY id")
    List<StoredFile> ordered();
    @Query("FROM StoredFile WHERE assetVersionId = :id ORDER BY id")
    List<StoredFile> forVersion(@Param("id") Long id);
}
