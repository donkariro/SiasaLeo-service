package com.arriyiaconsulting.siasaleo.service.domain.party.dto;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The minimum a role declaration has to supply before a person row can exist:
 * person (V1) requires both names. dateOfBirth and gender are optional here
 * because nothing verifies either — the eligibility rule that needs the date
 * belongs to the role (a voter must be 18 at registration), so the role's own
 * service decides whether a missing value is fatal, not this record. An
 * omitted gender is recorded as "not stated" rather than guessed (V41).
 */
public record ProfileDetails(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Past LocalDate dateOfBirth,
        Gender gender) {
}
