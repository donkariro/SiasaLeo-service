package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import java.util.List;

public record DigitalAssetDetailDto(DigitalAssetDto asset, DigitalAssetVersionDto version,
                                List<StoredFileDto> files) {}
