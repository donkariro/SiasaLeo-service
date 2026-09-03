package com.arriyiaconsulting.siasaleo.service.domain.politicalparty.control;

import com.arriyiaconsulting.siasaleo.service.domain.party.entity.Person;
import com.arriyiaconsulting.siasaleo.service.domain.party.repository.PersonRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.AppointOfficialRequest;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.dto.PoliticalPartyOfficialDto;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalParty;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.entity.PoliticalPartyOfficial;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyOfficialRepository;
import com.arriyiaconsulting.siasaleo.service.domain.politicalparty.repository.PoliticalPartyRepository;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Party offices: appoint an official and record when they stand down, keeping
 * closed tenures as the succession rather than overwriting them.
 * <p>
 * V19 carries no unique index, so unlike a voter registration or a party
 * membership nothing in the schema backs the checks here — they are the only
 * guarantee, and concurrent appointments could still slip past them. That is
 * deliberate on the schema's part: positions are free text and a party may
 * have several people holding the same one, so the only duplicate worth
 * refusing is the same person appointed twice to the same position.
 */
@ApplicationScoped
public class PoliticalPartyOfficialService {

    @Inject
    private PoliticalPartyOfficialRepository officials;

    @Inject
    private PersonRepository persons;

    @Inject
    private PoliticalPartyRepository politicalParties;

    @Transactional
    public PoliticalPartyOfficialDto appoint(AppointOfficialRequest request) {
        Person official = requirePerson(request.officialId());
        PoliticalParty party = requireParty(request.politicalPartyId());
        // @NotBlank has already run at the boundary, so trimming cannot empty
        // this; it only keeps ' Chairperson' from reading as a new position.
        String positionName = request.positionName().trim();
        if (officials.findCurrentTenure(official.getId(), party.getId(), positionName)
                .isPresent()) {
            throw new IllegalArgumentException("Person " + official.getId()
                    + " already holds '" + positionName + "' at party " + party.getId());
        }
        return PoliticalPartyOfficialDto.from(officials.save(
                new PoliticalPartyOfficial(official, party, positionName,
                        dateOrToday(request.fromDate()), request.photo(), request.about())));
    }

    @Transactional
    public PoliticalPartyOfficialDto stepDown(Long id, LocalDate uptoDate) {
        PoliticalPartyOfficial tenure = officials.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Party office tenure not found: " + id));
        if (!tenure.isCurrent()) {
            throw new IllegalArgumentException("Tenure " + id
                    + " already ended on " + tenure.getUptoDate());
        }
        LocalDate leftOn = dateOrToday(uptoDate);
        // fromDate is null where the register never recorded a start, and V19
        // permits an end date on its own in that case.
        if (tenure.getFromDate() != null && leftOn.isBefore(tenure.getFromDate())) {
            throw new IllegalArgumentException("Departure date " + leftOn
                    + " falls before " + tenure.getFromDate()
                    + ", the day the tenure began");
        }
        tenure.end(leftOn);
        return PoliticalPartyOfficialDto.from(officials.save(tenure));
    }

    public Optional<PoliticalPartyOfficialDto> findById(Long id) {
        return officials.findById(id).map(PoliticalPartyOfficialDto::from);
    }

    /**
     * A party's officials: those in office when current is true, otherwise the
     * whole succession including closed tenures.
     */
    public List<PoliticalPartyOfficialDto> findByParty(Long partyId, boolean current,
                                                       int page, int size) {
        requireParty(partyId);
        PageRequest pageRequest = pageRequest(page, size);
        List<PoliticalPartyOfficial> found = current
                ? officials.findCurrentByParty(partyId, pageRequest)
                : officials.findByParty(partyId, pageRequest);
        return found.stream().map(PoliticalPartyOfficialDto::from).toList();
    }

    /** Every party office the person has held, the most recent first. */
    public List<PoliticalPartyOfficialDto> findByPerson(Long personId) {
        requirePerson(personId);
        return officials.findByPerson(personId).stream()
                .map(PoliticalPartyOfficialDto::from)
                .toList();
    }

    private Person requirePerson(Long personId) {
        return persons.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));
    }

    private PoliticalParty requireParty(Long partyId) {
        return politicalParties.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Political party not found: " + partyId));
    }

    private static LocalDate dateOrToday(LocalDate date) {
        return date != null ? date : LocalDate.now();
    }

    // Jakarta Data pages are 1-based; the REST layer exposes 0-based pages.
    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.ofPage(page + 1L).size(size).withoutTotal();
    }
}
