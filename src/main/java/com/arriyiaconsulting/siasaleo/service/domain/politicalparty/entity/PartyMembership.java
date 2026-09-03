package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A person's membership of a political party over a period (V16). endDate is
 * null while the membership is current; resigning or defecting closes the row
 * rather than editing it, so the trail of who has belonged where survives.
 * <p>
 * The table records no reason for the ending, so a resignation and a defection
 * look alike here — they differ only in whether a new membership opens behind
 * them. Ranges are inclusive of both dates, and PartyMembershipService keeps
 * consecutive memberships from overlapping.
 */
@Entity
@Table(name = "party_membership")
public class PartyMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne(optional = false)
    @JoinColumn(name = "political_party_id")
    private PoliticalParty politicalParty;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    protected PartyMembership() {
    }

    public PartyMembership(Person person, PoliticalParty politicalParty, LocalDate startDate) {
        this.person = person;
        this.politicalParty = politicalParty;
        this.startDate = startDate;
    }

    /**
     * Closes this membership on its last day. The caller checks the date
     * against startDate first; the V16 CHECK is the backstop.
     */
    public void end(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isCurrent() {
        return endDate == null;
    }

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public PoliticalParty getPoliticalParty() {
        return politicalParty;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
