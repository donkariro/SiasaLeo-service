package com.arriyiaconsulting.siasaleo.service.security.identity.repository;

import com.arriyiaconsulting.siasaleo.service.security.identity.entity.VerificationCode;
import jakarta.data.Limit;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

@Repository
public interface VerificationCodeRepository extends BasicRepository<VerificationCode, Long> {

    // Newest first so callers can take the latest issued code with Limit.of(1);
    // resending simply issues a new row, which shadows earlier ones here.
    @Query("FROM VerificationCode WHERE userAccount.id = :accountId AND consumedAt IS NULL ORDER BY id DESC")
    List<VerificationCode> findActiveByAccount(@Param("accountId") Long accountId, Limit limit);
}
