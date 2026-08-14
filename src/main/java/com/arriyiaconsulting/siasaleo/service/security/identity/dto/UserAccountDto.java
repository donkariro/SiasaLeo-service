package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

import com.arriyiaconsulting.siasaleo.service.security.identity.entity.UserAccount;
import java.time.OffsetDateTime;

public record UserAccountDto(
        Long id,
        String username,
        String email,
        String phone,
        String status,
        OffsetDateTime createdAt) {

    public static UserAccountDto from(UserAccount account) {
        return new UserAccountDto(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.getPhone(),
                account.getStatus().name(),
                account.getCreatedAt());
    }
}
