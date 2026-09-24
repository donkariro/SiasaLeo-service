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

    @Column(name="ballot_name", length=255)
    private String ballotName;
    @Column(name="ballot_party_name", length=255)
    private String ballotPartyName;
    @Column(name="source_reference", length=2048)
    private String sourceReference;
    @Column(name="source_record_reference", length=1024)
    private String sourceRecordReference;
    @Column(name="import_fingerprint", length=64)
    private String importFingerprint;

    protected Candidacy() {
    }

    public Candidacy(Person person, Contest contest, PoliticalParty politicalParty,
                     CandidacyStatus status) {
        this.person = person;
        this.contest = contest;
        this.politicalParty = politicalParty;
        this.status = status;
        this.ballotName = person.getFirstName() + " " + person.getLastName();
        this.ballotPartyName = politicalParty == null ? null : politicalParty.getName();
    }

    public void recordBallot(String name, String partyName, String source, String record, String fingerprint) {
        if(name==null || name.isBlank() || name.length()>255 ||
                (politicalParty==null ? partyName!=null : partyName==null || partyName.isBlank() || partyName.length()>255))
            throw new IllegalArgumentException("Ballot name and election-time party label must match the candidacy affiliation");
        ballotName=name;
        ballotPartyName=partyName;
        sourceReference=source;
        sourceRecordReference=record;
        importFingerprint=fingerprint;
    }
    public String getBallotName() { return ballotName; }
    public String getBallotPartyName() { return ballotPartyName; }
    public String getSourceReference() { return sourceReference; }
    public String getSourceRecordReference() { return sourceRecordReference; }
    public String getImportFingerprint() { return importFingerprint; }

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
