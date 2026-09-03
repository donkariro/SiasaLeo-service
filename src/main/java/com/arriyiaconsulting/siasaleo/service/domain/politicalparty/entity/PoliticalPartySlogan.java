package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity;

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
 * A slogan a political party has campaigned under (V20). uptoDate is null
 * while the slogan is in use; retiring one closes the row rather than editing
 * it. Unlike a symbol there is no unique index, so a party may run several
 * slogans at once — adopting one leaves the others alone.
 */
@Entity
@Table(name = "politicalparty_slogan")
public class PoliticalPartySlogan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "politicalparty_id")
    private PoliticalParty politicalParty;

    @Column(nullable = false, length = 255)
    private String slogan;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "upto_date")
    private LocalDate uptoDate;

    protected PoliticalPartySlogan() {
    }

    public PoliticalPartySlogan(PoliticalParty politicalParty, String slogan,
                                LocalDate fromDate) {
        this.politicalParty = politicalParty;
        this.slogan = slogan;
        this.fromDate = fromDate;
    }

    /**
     * Closes this slogan on its last day in use. The caller checks the date
     * against fromDate first; the V20 CHECK is the backstop.
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

    public PoliticalParty getPoliticalParty() {
        return politicalParty;
    }

    public String getSlogan() {
        return slogan;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getUptoDate() {
        return uptoDate;
    }
}
