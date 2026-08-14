package com.arriyiaconsulting.siasaleo.service.security.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Identifies the pending account the same way it registered, plus the code it received. */
public record VerifyRequest(
        @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 10) String code) {
}
