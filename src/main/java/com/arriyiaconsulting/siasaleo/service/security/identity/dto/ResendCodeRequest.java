package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

import jakarta.validation.constraints.Size;

public record ResendCodeRequest(
        @Size(max = 255) String email,
        @Size(max = 30) String phone) {
}
