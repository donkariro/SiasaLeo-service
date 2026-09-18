package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.mapping.PoliticalPartyMapper;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.AdoptSymbolRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartySymbolDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartySymbol;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartySymbolRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * A party's ballot symbol. idx_politicalparty_symbol_current allows exactly
 * one open row per party, so adopting a symbol is a replacement: the symbol in
 * use is closed the day before the new one comes in, and the old row is kept
 * as history rather than overwritten.
 * <p>
 * Ranges are inclusive of both dates, as for a party membership, so a symbol
 * closed on the day its successor starts would leave the party showing two
 * symbols that day. Hence a new symbol must start after the day the current
 * one did — unless the register never recorded that day, which V20 allows and
 * which leaves nothing to compare against.
 * <p>
 * There is no operation to drop a symbol without a replacement: a party on a
 * ballot has one, and the index exists to keep it that way.
 */
@ApplicationScoped
public class PoliticalPartySymbolService {

    @Inject
    private PoliticalPartyMapper politicalPartyMapper;

    @Inject
    private PoliticalPartySymbolRepository symbols;

    @Inject
    private PoliticalPartyRepository politicalParties;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public PoliticalPartySymbolDto adopt(Long partyId, AdoptSymbolRequest request) {
        PoliticalParty party = requireParty(partyId);
        LocalDate fromDate = dateOrToday(request.fromDate());
        Optional<PoliticalPartySymbol> inUse = symbols.findCurrentByParty(party.getId());

        if (inUse.isPresent()) {
            PoliticalPartySymbol current = inUse.get();
            if (current.getFromDate() != null && !fromDate.isAfter(current.getFromDate())) {
                throw new IllegalArgumentException("Symbol date " + fromDate
                        + " must fall after " + current.getFromDate()
                        + ", the day the current symbol came into use");
            }
            current.end(fromDate.minusDays(1));
            symbols.save(current);
            // Hibernate flushes inserts ahead of updates, so without forcing
            // the update out first the new open row would hit
            // idx_politicalparty_symbol_current while the old one is open.
            entityManager.flush();
        }

        return politicalPartyMapper.toPoliticalPartySymbolDto(symbols.save(politicalPartyMapper.toSymbol(request, party, fromDate)));
    }

    /** The symbol on the ballot, absent for a party the register gave none. */
    public Optional<PoliticalPartySymbolDto> findCurrent(Long partyId) {
        requireParty(partyId);
        return symbols.findCurrentByParty(partyId).map(politicalPartyMapper::toPoliticalPartySymbolDto);
    }

    /** Every symbol the party has used, the most recent first. */
    public List<PoliticalPartySymbolDto> findHistory(Long partyId) {
        requireParty(partyId);
        return symbols.findByParty(partyId).stream()
                .map(politicalPartyMapper::toPoliticalPartySymbolDto)
                .toList();
    }

    private PoliticalParty requireParty(Long partyId) {
        return politicalParties.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Political party not found: " + partyId));
    }

    private static LocalDate dateOrToday(LocalDate date) {
        return date != null ? date : LocalDate.now();
    }
}
