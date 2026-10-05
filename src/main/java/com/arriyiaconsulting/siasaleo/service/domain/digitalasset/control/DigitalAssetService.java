package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.mapping.DigitalAssetMapper;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.control.GovernanceRules;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;

@ApplicationScoped
public class DigitalAssetService {
    @Inject private DigitalAssetRepository assets;
    @Inject private DigitalAssetVersionRepository versions;
    @Inject private StoredFileRepository files;
    @Inject private DigitalAssetMapper mapper;

    @Transactional
    public DigitalAssetDto create(CreateDigitalAssetRequest request, long actor) {
        DigitalAsset asset = mapper.toEntity(request);
        asset.setCreatedByAccountId(actor);
        return mapper.toDto(assets.insert(asset));
    }

    private DigitalAsset requireOwner(long assetId, long actor) {
        DigitalAsset asset = assets.findById(assetId).orElseThrow(RecordNotFoundException::new);
        if (!Long.valueOf(actor).equals(asset.getCreatedByAccountId())) throw new RecordAccessDeniedException();
        return asset;
    }

    @Transactional
    public DigitalAssetVersionDto version(long assetId, CreateDigitalAssetVersionRequest request, long actor) {
        requireOwner(assetId, actor);
        DigitalAssetVersion version = mapper.toEntity(request);
        version.setAssetId(assetId);
        version.setVersionNumber(GovernanceRules.next(versions.latestNumber(assetId)));
        version.setRecordedByAccountId(actor);
        return mapper.toDto(versions.insert(version));
    }

    @Transactional
    public StoredFileDto file(long versionId, CreateStoredFileRequest request, long actor) {
        DigitalAssetVersion version = requireVersion(versionId);
        requireOwner(version.getAssetId(), actor);
        requireUnsealed(version);
        StoredFile file = mapper.toEntity(request);
        file.setAssetVersionId(versionId);
        return mapper.toDto(files.insert(file));
    }

    @Transactional
    public DigitalAssetDetailDto seal(long versionId, long actor) {
        DigitalAssetVersion version = requireVersion(versionId);
        requireOwner(version.getAssetId(), actor);
        requireUnsealed(version);
        if (version.getSourceUrl() == null && files.forVersion(versionId).isEmpty())
            throw new IllegalArgumentException("An asset version needs an external source or stored file");
        version.setSealedAt(OffsetDateTime.now());
        versions.update(version);
        return read(versionId, actor);
    }

    public DigitalAssetDetailDto read(long versionId, Long actor) {
        DigitalAssetVersion version = requireVersion(versionId);
        DigitalAsset asset;
        if (actor != null) asset = requireOwner(version.getAssetId(), actor);
        else {
            if (version.getSealedAt() == null) throw new RecordNotFoundException();
            asset = assets.findById(version.getAssetId()).orElseThrow(RecordNotFoundException::new);
        }
        return new DigitalAssetDetailDto(mapper.toDto(asset), mapper.toDto(version),
                files.forVersion(versionId).stream().map(mapper::toDto).toList());
    }

    public DigitalAssetVersion requireSealed(long versionId) {
        DigitalAssetVersion version = requireVersion(versionId);
        if (version.getSealedAt() == null) throw new IllegalArgumentException("Evidence needs a sealed asset version");
        return version;
    }
    private DigitalAssetVersion requireVersion(long id) { return versions.findById(id).orElseThrow(RecordNotFoundException::new); }
    private void requireUnsealed(DigitalAssetVersion version) {
        if (version.getSealedAt() != null) throw new IllegalArgumentException("Sealed assets require a new version");
    }
}
