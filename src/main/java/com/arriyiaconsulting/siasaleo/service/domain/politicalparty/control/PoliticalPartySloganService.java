package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.AdoptSloganRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartySloganDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartySlogan;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartySloganRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

/**
 * A party's slogans. V20 puts no unique index over them, so unlike a symbol
 * several may be in use at once: adopting one leaves the rest alone, and each
 * is retired on its own. Retiring closes the row rather than deleting it, so
 * what a party has campaigned under survives.
 */
@ApplicationScoped
public class PoliticalPartySloganService {

    @Inject
    private PoliticalPartySloganRepository slogans;

    @Inject
    private PoliticalPartyRepository politicalParties;

    @Transactional
    public PoliticalPartySloganDto adopt(Long partyId, AdoptSloganRequest request) {
        PoliticalParty party = requireParty(partyId);
        return PoliticalPartySloganDto.from(slogans.save(new PoliticalPartySlogan(
                party, request.slogan().trim(), dateOrToday(request.fromDate()))));
    }

    /**
     * Takes one slogan out of use. The party is part of the address so a
     * slogan cannot be retired through the wrong party.
     */
    @Transactional
    public PoliticalPartySloganDto retire(Long partyId, Long sloganId, LocalDate uptoDate) {
        requireParty(partyId);
        PoliticalPartySlogan slogan = slogans.findById(sloganId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Slogan not found: " + sloganId));
        if (!slogan.getPoliticalParty().getId().equals(partyId)) {
            throw new IllegalArgumentException("Slogan " + sloganId
                    + " does not belong to party " + partyId);
        }
        if (!slogan.isCurrent()) {
            throw new IllegalArgumentException("Slogan " + sloganId
                    + " was already retired on " + slogan.getUptoDate());
        }
        LocalDate retiredOn = dateOrToday(uptoDate);
        // fromDate is null where the register never recorded a start, and V20
        // permits an end date on its own in that case.
        if (slogan.getFromDate() != null && retiredOn.isBefore(slogan.getFromDate())) {
            throw new IllegalArgumentException("Retirement date " + retiredOn
                    + " falls before " + slogan.getFromDate()
                    + ", the day the slogan came into use");
        }
        slogan.end(retiredOn);
        return PoliticalPartySloganDto.from(slogans.save(slogan));
    }

    /**
     * The party's slogans: those in use when current is true, otherwise every
     * slogan on record. Most recent first.
     */
    public List<PoliticalPartySloganDto> findByParty(Long partyId, boolean current) {
        requireParty(partyId);
        List<PoliticalPartySlogan> found = current
                ? slogans.findCurrentByParty(partyId)
                : slogans.findByParty(partyId);
        return found.stream().map(PoliticalPartySloganDto::from).toList();
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
