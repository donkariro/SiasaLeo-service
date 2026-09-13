package com.arriyiaconsulting.siasaleo.service.domain.party.dto;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.PersonClaim;
import java.time.OffsetDateTime;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A claim as the reviewer and the claimant see it. evidence is included
 * because the review queue is read to decide, not merely to list.
 */
public record PersonClaimDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long userAccountId,
        @Schema(required = true) Long personId,
        @Schema(required = true) String firstName,
        @Schema(required = true) String lastName,
        @Schema(required = true) String status,
        String evidence,
        @Schema(required = true) OffsetDateTime submittedAt,
        OffsetDateTime decidedAt,
        Long decidedBy,
        String decisionNote) {

    public static PersonClaimDto from(PersonClaim claim) {
        Person person = claim.getPerson();
        return new PersonClaimDto(claim.getId(), claim.getUserAccountId(),
                person.getId(), person.getFirstName(), person.getLastName(),
                claim.getStatus().name(), claim.getEvidence(), claim.getSubmittedAt(),
                claim.getDecidedAt(), claim.getDecidedBy(), claim.getDecisionNote());
    }
}
