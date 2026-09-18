package com.arriyiaconsulting.siasaleo.service.domain.office.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.office.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.office.entity.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.Mapper;

@Mapper(config = MappingConfig.class)
public interface OfficeMapper {

    OfficeDto toOfficeDto(Office entity);

    SeatDto toSeatDto(Seat entity);
}
