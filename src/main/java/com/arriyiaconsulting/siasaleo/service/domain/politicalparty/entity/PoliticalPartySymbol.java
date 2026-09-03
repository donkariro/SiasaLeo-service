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
 * A symbol a political party has presented on the ballot (V20). uptoDate is
 * null while the symbol is in use, and idx_politicalparty_symbol_current
 * enforces that a party has exactly one such row: adopting a new symbol closes
 * the old one rather than editing it, so the succession survives.
 * <p>
 * fromDate may be null, as for a party office: the register accommodates
 * records whose start was never captured, and the V20 CHECK allows it.
 */
@Entity
@Table(name = "politicalparty_symbol")
public class PoliticalPartySymbol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "politicalparty_id")
    private PoliticalParty politicalParty;

    @Column(name = "symbol_description", nullable = false, length = 255)
    private String symbolDescription;

    // File name of the symbol artwork, carried over from the ORPP register.
    @Column(name = "image_file", length = 255)
    private String imageFile;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "upto_date")
    private LocalDate uptoDate;

    protected PoliticalPartySymbol() {
    }

    public PoliticalPartySymbol(PoliticalParty politicalParty, String symbolDescription,
                                String imageFile, LocalDate fromDate) {
        this.politicalParty = politicalParty;
        this.symbolDescription = symbolDescription;
        this.imageFile = imageFile;
        this.fromDate = fromDate;
    }

    /**
     * Closes this symbol on its last day in use. The caller checks the date
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

    public String getSymbolDescription() {
        return symbolDescription;
    }

    public String getImageFile() {
        return imageFile;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getUptoDate() {
        return uptoDate;
    }
}
