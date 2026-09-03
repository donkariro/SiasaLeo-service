package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartySlogan;
import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record PoliticalPartySloganDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long politicalPartyId,
        @Schema(required = true) String slogan,
        // Null where the register never recorded when the slogan came in.
        @Schema(required = true, nullable = true) LocalDate fromDate,
        // Null while the slogan is still in use.
        @Schema(required = true, nullable = true) LocalDate uptoDate,
        @Schema(required = true) boolean current) {

    public static PoliticalPartySloganDto from(PoliticalPartySlogan slogan) {
        return new PoliticalPartySloganDto(slogan.getId(),
                slogan.getPoliticalParty().getId(), slogan.getSlogan(),
                slogan.getFromDate(), slogan.getUptoDate(), slogan.isCurrent());
    }
}
