package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartyColorsDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.ReplaceColorsRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartyColor;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyColorRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * A party's colours. V20 gave them no validity period — they are a plain
 * multi-valued attribute held in register order — so there is nothing to open
 * and close: the set is replaced wholesale and display_order is rewritten
 * 1..n from the order the caller gives.
 * <p>
 * Colours are free text and carry no unique constraint, so repeats are stored
 * as given rather than refused; V20 itself carried tokens like 'Colourless'
 * across verbatim.
 */
@ApplicationScoped
public class PoliticalPartyColorService {

    // display_order is 1-based, matching the ordinality V20 assigned.
    private static final int FIRST_DISPLAY_ORDER = 1;

    @Inject
    private PoliticalPartyColorRepository colors;

    @Inject
    private PoliticalPartyRepository politicalParties;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public PoliticalPartyColorsDto replace(Long partyId, ReplaceColorsRequest request) {
        PoliticalParty party = requireParty(partyId);
        List<PoliticalPartyColor> existing = colors.findByParty(party.getId());
        if (!existing.isEmpty()) {
            colors.deleteAll(existing);
            // The deletes carry no constraint the inserts could trip, but
            // flushing keeps the old rows from outliving the new ones in the
            // persistence context and being re-read by the query below.
            entityManager.flush();
        }

        List<PoliticalPartyColor> replacements = new ArrayList<>();
        int displayOrder = FIRST_DISPLAY_ORDER;
        for (String colorName : request.colors()) {
            replacements.add(colors.save(
                    new PoliticalPartyColor(party, colorName.trim(), displayOrder++)));
        }
        return toDto(party.getId(), replacements);
    }

    /** The party's colours in register order; empty where none are recorded. */
    public PoliticalPartyColorsDto findByParty(Long partyId) {
        requireParty(partyId);
        return toDto(partyId, colors.findByParty(partyId));
    }

    private static PoliticalPartyColorsDto toDto(Long partyId,
                                                 List<PoliticalPartyColor> found) {
        return new PoliticalPartyColorsDto(partyId, found.stream()
                .map(PoliticalPartyColor::getColorName)
                .toList());
    }

    private PoliticalParty requireParty(Long partyId) {
        return politicalParties.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Political party not found: " + partyId));
    }
}
