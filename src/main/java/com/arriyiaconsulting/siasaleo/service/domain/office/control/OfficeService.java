package com.arriyiaconsulting.siasaleo.service.domain.office.control;

import com.arriyiaconsulting.siasaleo.service.domain.office.dto.OfficeDto;
import com.arriyiaconsulting.siasaleo.service.domain.office.repository.OfficeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

/** Read access to the office register for candidate and other selection flows. */
@ApplicationScoped
public class OfficeService {

    @Inject
    private OfficeRepository offices;

    /** The complete, small office register in alphabetical order, without pagination. */
    public List<OfficeDto> findAll() {
        return offices.findAllOrderedByName().stream().map(OfficeDto::from).toList();
    }

    public Optional<OfficeDto> findById(Long id) {
        return offices.findById(id).map(OfficeDto::from);
    }
}
