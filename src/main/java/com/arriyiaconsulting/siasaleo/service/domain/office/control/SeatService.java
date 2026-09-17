package com.arriyiaconsulting.siasaleo.service.domain.office.control;

import com.arriyiaconsulting.siasaleo.service.domain.office.dto.SeatDto;
import com.arriyiaconsulting.siasaleo.service.domain.office.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.repository.ElectoralAreaRepository;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SeatService {
    @Inject private SeatRepository seats;
    @Inject private OfficeRepository offices;
    @Inject private ElectoralAreaRepository areas;

    public Optional<SeatDto> findById(Long id) {
        return seats.findById(id).map(SeatDto::from);
    }

    public List<SeatDto> search(Long officeId, Long areaId, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("Page must be zero or greater");
        if (officeId != null) {
            offices.findById(officeId).orElseThrow(() -> new IllegalArgumentException("Office not found: " + officeId));
        }
        if (areaId != null) {
            areas.findById(areaId).orElseThrow(() -> new IllegalArgumentException("Electoral area not found: " + areaId));
        }
        return seats.search(officeId, areaId,
                PageRequest.ofPage(page + 1L).size(Math.max(1, Math.min(size, 500))).withoutTotal())
                .stream().map(SeatDto::from).toList();
    }
}
