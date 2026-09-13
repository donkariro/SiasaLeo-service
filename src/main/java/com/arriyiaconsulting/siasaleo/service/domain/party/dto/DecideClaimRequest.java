package com.arriyiaconsulting.siasaleo.service.domain.party.dto;

import jakarta.validation.constraints.Size;

/**
 * A reviewer's note on an approval or rejection. The verb is the endpoint, not
 * a field: a payload that carried the outcome would make "approve" and
 * "reject" the same authorization decision.
 */
public record DecideClaimRequest(@Size(max = 500) String note) {
}
