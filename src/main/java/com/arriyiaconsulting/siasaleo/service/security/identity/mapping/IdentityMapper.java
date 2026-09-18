package com.arriyiaconsulting.siasaleo.service.security.identity.mapping;

import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import com.arriyiaconsulting.siasaleo.service.security.identity.dto.*;
import com.arriyiaconsulting.siasaleo.service.security.identity.entity.*;
import org.mapstruct.Mapper;

@Mapper(config = MappingConfig.class)
public interface IdentityMapper {

    UserAccountDto toUserAccountDto(UserAccount entity);
}
