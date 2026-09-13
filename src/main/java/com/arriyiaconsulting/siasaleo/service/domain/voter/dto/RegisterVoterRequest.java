package com.arriyiaconsulting.siasaleo.service.domain.voter.dto;

import com.arriyiaconsulting.siasaleo.service.domain.party.dto.ProfileDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/**
 * Declares the caller a voter at a centre. The person is never named here: it
 * is the authenticated account's, so this payload cannot register somebody
 * else.
 *
 * profile carries the names the party model needs and is required only on an
 * account that has no person yet — the usual case, since declaring as a voter
 * is what creates one. An account that already has a person (an approved
 * aspirant claim, say) omits it, and it is ignored if sent.
 *
 * registrationCenterId must be an electoral area of type REGISTRATION_CENTER.
 * registrationDate is optional and defaults to today, so recording a
 * registration as it happens needs no date; supplying one is for backfilling.
 */
public record RegisterVoterRequest(
        @NotNull Long registrationCenterId,
        @Valid ProfileDetails profile,
        @PastOrPresent LocalDate registrationDate) {
}
