package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Shared Jakarta CDI configuration; missing target mappings fail compilation.
 * Creation mappings use public domain constructors and relationships resolved by
 * services. Domain methods retain responsibility for updates and state changes.
 */
@MapperConfig(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MappingConfig {
}
