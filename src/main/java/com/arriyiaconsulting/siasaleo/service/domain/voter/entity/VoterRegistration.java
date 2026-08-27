package com.arriyiaconsulting.siasaleo.service.domain.voter.entity;

import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.entity.ElectoralArea;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Where a person is registered to vote (V15). registrationCenter must be an
 * electoral area of type REGISTRATION_CENTER — enforced by
 * VoterRegistrationService, not the schema. A person holds at most one ACTIVE
 * row; transferring or deregistering retires the current one rather than
 * editing it, so the trail of where someone has been registered survives.
 */
@Entity
@Table(name = "voter_registration")
public class VoterRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne(optional = false)
    @JoinColumn(name = "registration_center_id")
    private ElectoralArea registrationCenter;

    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoterRegistrationStatus status;

    protected VoterRegistration() {
    }

    public VoterRegistration(Person person, ElectoralArea registrationCenter,
                             LocalDate registrationDate) {
        this.person = person;
        this.registrationCenter = registrationCenter;
        this.registrationDate = registrationDate;
        this.status = VoterRegistrationStatus.ACTIVE;
    }

    /** Retires this registration because the voter moved to another centre. */
    public void transferAway() {
        status = VoterRegistrationStatus.TRANSFERRED;
    }

    /** Retires this registration without a replacement. */
    public void deregister() {
        status = VoterRegistrationStatus.DEREGISTERED;
    }

    public boolean isActive() {
        return status == VoterRegistrationStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public ElectoralArea getRegistrationCenter() {
        return registrationCenter;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public VoterRegistrationStatus getStatus() {
        return status;
    }
}
