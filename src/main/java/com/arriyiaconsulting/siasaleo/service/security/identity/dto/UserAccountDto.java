package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

import java.time.OffsetDateTime;

public record UserAccountDto(
        Long id,
        String username,
        String email,
        String phone,
        String status,
        OffsetDateTime createdAt) {
}
