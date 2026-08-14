package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sign-up payload. Exactly one of email or phone must be supplied; that rule
 * and the format checks are business outcomes (RegistrationResult variants)
 * decided by RegistrationService, so bean validation here is only structural.
 */
public record RegisterRequest(
        @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 255) String password,
        @NotBlank @Size(max = 255) String passwordConfirmation) {
}
