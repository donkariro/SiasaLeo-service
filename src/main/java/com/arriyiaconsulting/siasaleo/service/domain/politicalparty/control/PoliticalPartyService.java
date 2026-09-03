package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartyDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Read access to the register of fully registered political parties, seeded
 * from the ORPP register by V10. Parties are not created or amended through
 * the API yet, so everything here is read-only.
 */
@ApplicationScoped
public class PoliticalPartyService {

    @Inject
    private PoliticalPartyRepository politicalParties;

    public Optional<PoliticalPartyDto> findById(Long id) {
        return politicalParties.findById(id).map(PoliticalPartyDto::from);
    }

    /** The register in name order. */
    public List<PoliticalPartyDto> findAll(int page, int size) {
        return politicalParties.findAllOrderedByName(pageRequest(page, size)).stream()
                .map(PoliticalPartyDto::from)
                .toList();
    }

    // Jakarta Data pages are 1-based; the REST layer exposes 0-based pages.
    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.ofPage(page + 1L).size(size).withoutTotal();
    }
}
