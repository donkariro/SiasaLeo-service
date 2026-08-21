package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login payload. Exactly one of email or phone must be supplied; that rule is
 * a business outcome (LoginResult variant) decided by AuthenticationService,
 * so bean validation here is only structural.
 */
public record LoginRequest(
        @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 255) String password) {
}
