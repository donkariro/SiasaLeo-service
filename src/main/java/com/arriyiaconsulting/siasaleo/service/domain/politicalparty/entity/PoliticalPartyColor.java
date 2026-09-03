package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One of a political party's colours (V20). Unlike a symbol or a slogan this
 * carries no validity period: colours are a plain multi-valued attribute kept
 * in the order the ORPP register lists them, so the set is replaced wholesale
 * rather than opened and closed row by row.
 */
@Entity
@Table(name = "politicalparty_color")
public class PoliticalPartyColor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "politicalparty_id")
    private PoliticalParty politicalParty;

    @Column(name = "color_name", nullable = false, length = 100)
    private String colorName;

    // 1-based, matching the ordinality V20 assigned when it split the old
    // free-text colors column.
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected PoliticalPartyColor() {
    }

    public PoliticalPartyColor(PoliticalParty politicalParty, String colorName,
                               int displayOrder) {
        this.politicalParty = politicalParty;
        this.colorName = colorName;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public PoliticalParty getPoliticalParty() {
        return politicalParty;
    }

    public String getColorName() {
        return colorName;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
