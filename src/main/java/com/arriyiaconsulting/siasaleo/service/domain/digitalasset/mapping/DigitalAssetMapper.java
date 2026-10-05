package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.mapping;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.MappingConfig;
import org.mapstruct.*;

@Mapper(config = MappingConfig.class)
public interface DigitalAssetMapper {
    DigitalAssetDto toDto(DigitalAsset entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "assetType", source = "assetType")
    DigitalAsset toEntity(CreateDigitalAssetRequest request);

    DigitalAssetVersionDto toDto(DigitalAssetVersion entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "sourceUrl", source = "sourceUrl")
    @Mapping(target = "authorPartyId", source = "authorPartyId")
    @Mapping(target = "publisherPartyId", source = "publisherPartyId")
    @Mapping(target = "sourcePublishedOn", source = "sourcePublishedOn")
    @Mapping(target = "provenance", source = "provenance")
    DigitalAssetVersion toEntity(CreateDigitalAssetVersionRequest request);

    StoredFileDto toDto(StoredFile entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "representationType", source = "representationType")
    @Mapping(target = "storageKey", source = "storageKey")
    @Mapping(target = "originalFilename", source = "originalFilename")
    @Mapping(target = "mediaType", source = "mediaType")
    @Mapping(target = "byteSize", source = "byteSize")
    @Mapping(target = "sha256", source = "sha256")
    StoredFile toEntity(CreateStoredFileRequest request);

    PersonDigitalAssetDto toDto(PersonDigitalAsset entity);

    DigitalAssetTypeDto toDto(DigitalAssetType entity);

}
