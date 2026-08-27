package com.arriyiaconsulting.siasaleo.service.domain.voter.dto;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralArea;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.voter.entity.VoterRegistration;
import java.time.LocalDate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record VoterRegistrationDto(
        @Schema(required = true) Long id,
        @Schema(required = true) Long personId,
        @Schema(required = true) String firstName,
        @Schema(required = true) String lastName,
        @Schema(required = true) Long registrationCenterId,
        @Schema(required = true) String registrationCenterName,
        @Schema(required = true) LocalDate registrationDate,
        @Schema(required = true) String status) {

    public static VoterRegistrationDto from(VoterRegistration registration) {
        Person person = registration.getPerson();
        ElectoralArea center = registration.getRegistrationCenter();
        return new VoterRegistrationDto(registration.getId(), person.getId(),
                person.getFirstName(), person.getLastName(),
                center.getId(), center.getName(),
                registration.getRegistrationDate(), registration.getStatus().name());
    }
}
