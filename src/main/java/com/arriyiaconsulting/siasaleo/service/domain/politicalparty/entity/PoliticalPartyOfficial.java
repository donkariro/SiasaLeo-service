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
 * A person's tenure as an official of a political party — chairperson,
 * secretary-general, treasurer and so on (V19). uptoDate is null while the
 * person holds the position; standing down closes the row rather than editing
 * it, so the succession survives.
 * <p>
 * Unlike a party membership, fromDate may be null: the register accommodates
 * historical records whose start is unknown, and the V19 CHECK is written to
 * allow it. positionName is free text rather than a lookup, and the schema
 * carries no unique index over it — a party may have several people holding
 * the same position at once.
 */
@Entity
@Table(name = "politicalparty_official")
public class PoliticalPartyOfficial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "official_id")
    private Person official;

    @ManyToOne(optional = false)
    @JoinColumn(name = "politicalparty_id")
    private PoliticalParty politicalParty;

    @Column(name = "position_name", nullable = false, length = 100)
    private String positionName;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "upto_date")
    private LocalDate uptoDate;

    // File name of the official's portrait, as with a party's symbol image.
    @Column(length = 255)
    private String photo;

    // TEXT in V19: a biographical note with no length ceiling.
    @Column
    private String about;

    protected PoliticalPartyOfficial() {
    }

    public PoliticalPartyOfficial(Person official, PoliticalParty politicalParty,
                                  String positionName, LocalDate fromDate,
                                  String photo, String about) {
        this.official = official;
        this.politicalParty = politicalParty;
        this.positionName = positionName;
        this.fromDate = fromDate;
        this.photo = photo;
        this.about = about;
    }

    /**
     * Closes this tenure on its last day. The caller checks the date against
     * fromDate first; the V19 CHECK is the backstop.
     */
    public void end(LocalDate uptoDate) {
        this.uptoDate = uptoDate;
    }

    public boolean isCurrent() {
        return uptoDate == null;
    }

    public Long getId() {
        return id;
    }

    public Person getOfficial() {
        return official;
    }

    public PoliticalParty getPoliticalParty() {
        return politicalParty;
    }

    public String getPositionName() {
        return positionName;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getUptoDate() {
        return uptoDate;
    }

    public String getPhoto() {
        return photo;
    }

    public String getAbout() {
        return about;
    }
}
