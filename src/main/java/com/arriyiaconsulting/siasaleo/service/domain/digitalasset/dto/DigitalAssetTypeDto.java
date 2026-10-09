package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import jakarta.json.bind.annotation.JsonbProperty;

public record DigitalAssetTypeDto(
    @JsonbProperty("code") String code,
    @JsonbProperty("name") String name,
    @JsonbProperty("active") Boolean active
) {}
