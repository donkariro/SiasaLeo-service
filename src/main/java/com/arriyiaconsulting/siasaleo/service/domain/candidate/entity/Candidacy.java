package com.arriyiaconsulting.siasaleo.service.domain.candidate.entity;

import com.arriyiaconsulting.siasaleo.service.domain.election.entity.Contest;
import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A person running in a contest (V11), normally sponsored by a political
 * party; politicalParty is null for independent candidates. status walks the
 * V21 lifecycle, starting at EXPRESSED_INTEREST (see CandidacyService).
 */
@Entity
@Table(name = "candidacy")
public class Candidacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private Contest contest;

    // Read-only view of the FK so callers can get the contest id without
    // touching the lazy association.
    @Column(name = "contest_id", insertable = false, updatable = false)
    private Long contestId;

    @ManyToOne
    @JoinColumn(name = "political_party_id")
    private PoliticalParty politicalParty;

    @ManyToOne(optional = false)
    @JoinColumn(name = "status")
    private CandidacyStatus status;

    protected Candidacy() {
    }

    public Candidacy(Person person, Contest contest, PoliticalParty politicalParty,
                     CandidacyStatus status) {
        this.person = person;
        this.contest = contest;
        this.politicalParty = politicalParty;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public Long getContestId() {
        if (contestId != null) {
            return contestId;
        }
        return contest != null ? contest.getId() : null;
    }

    public PoliticalParty getPoliticalParty() {
        return politicalParty;
    }

    public CandidacyStatus getStatus() {
        return status;
    }
}
