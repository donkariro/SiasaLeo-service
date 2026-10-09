package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.repository.*;
import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.mapping.DigitalAssetMapper;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.data.Limit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/** Shared media identity, authorization and lifecycle. Consumers own usage rules. */
@ApplicationScoped
public class DigitalAssetService {
    @Inject private DigitalAssetRepository assets;
    @Inject private DigitalAssetVersionRepository versions;
    @Inject private StoredFileRepository files;
    @Inject private DigitalAssetTypeRepository types;
    @Inject private AssetManagementRepository management;
    @Inject private DigitalAssetMapper mapper;

    public List<DigitalAssetTypeDto> types() { return types.ordered().stream().map(mapper::toDto).toList(); }
    @Transactional
    public DigitalAssetDto create(CreateDigitalAssetRequest request, long actor) {
        requireType(request.assetType());
        DigitalAsset asset = mapper.toEntity(request);
        asset.setCreatedByAccountId(actor);
        if (request.ownerPartyId() != null) {
            requirePartyManager(request.ownerPartyId(), actor);
            asset.setOwnerPartyId(request.ownerPartyId());
        } else asset.setOwnerAccountId(actor);
        management.actor(actor);
        return mapper.toDto(assets.insert(asset));
    }
    public boolean owns(DigitalAsset asset, long actor) {
        return Long.valueOf(actor).equals(asset.getOwnerAccountId())
            || (asset.getOwnerPartyId() != null && management.partyManager(asset.getOwnerPartyId(), actor));
    }
    private boolean manages(DigitalAsset asset, long actor) {
        return owns(asset, actor) || management.hasGrant(asset.getId(), actor, true);
    }
    private boolean readsPrivate(DigitalAsset asset, Long actor) {
        return actor != null && (owns(asset, actor) || management.hasGrant(asset.getId(), actor, false));
    }
    private DigitalAsset writable(long id, long actor, boolean content) {
        DigitalAsset asset = management.lock(id).orElseThrow(RecordNotFoundException::new);
        if (!manages(asset, actor)) throw new RecordAccessDeniedException();
        if (content && asset.getArchivedAt() != null) throw new IllegalArgumentException("Restore the archived asset first");
        management.actor(actor);
        return asset;
    }
    private DigitalAsset owned(long id, long actor) {
        DigitalAsset asset = management.lock(id).orElseThrow(RecordNotFoundException::new);
        if (!owns(asset, actor)) throw new RecordAccessDeniedException();
        management.actor(actor);
        return asset;
    }
    @Transactional
    public DigitalAssetVersionDto version(long assetId, CreateDigitalAssetVersionRequest request, long actor) {
        writable(assetId, actor, true); // Serializes MAX+1 across writers.
        DigitalAssetVersion version = mapper.toEntity(request);
        version.setAssetId(assetId);
        Integer latest = versions.latestNumber(assetId);
        version.setVersionNumber(latest == null ? 1 : Math.addExact(latest, 1));
        version.setRecordedByAccountId(actor);
        return mapper.toDto(versions.insert(version));
    }
    /** Metadata registration only; actual file transfers remain deferred. */
    @Transactional
    public StoredFileDto file(long versionId, CreateStoredFileRequest request, long actor) {
        DigitalAssetVersion version = requireVersion(versionId);
        writable(version.getAssetId(), actor, true);
        requireUnsealed(version);
        StoredFile file = mapper.toEntity(request);
        file.setAssetVersionId(versionId);
        return mapper.toDto(files.insert(file));
    }
    @Transactional
    public DigitalAssetDetailDto seal(long versionId, long actor) {
        DigitalAssetVersion version = requireVersion(versionId);
        DigitalAsset asset = writable(version.getAssetId(), actor, true);
        requireUnsealed(version);
        if (version.getSourceUrl() == null && files.forVersion(versionId).isEmpty())
            throw new IllegalArgumentException("An asset version needs an external source or stored file");
        version.snapshot(asset);
        version.setSealedAt(OffsetDateTime.now());
        versions.update(version);
        return read(versionId, actor);
    }
    public DigitalAssetDetailDto read(long versionId, Long actor) {
        DigitalAssetVersion version = requireVersion(versionId);
        DigitalAsset asset = requireAsset(version.getAssetId());
        boolean privileged = readsPrivate(asset, actor);
        if (!privileged && (asset.getVisibility() != AssetVisibility.PUBLIC || version.getSealedAt() == null))
            throw new RecordNotFoundException();
        // READ grants cover sealed contents; drafts require management authority.
        if (version.getSealedAt() == null && (actor == null || !manages(asset, actor)))
            throw new RecordNotFoundException();
        return detail(asset, version);
    }
    public DigitalAssetDetailDto current(long assetId, Long actor) {
        DigitalAsset asset = readableAsset(assetId, actor);
        DigitalAssetVersion version = versions.sealedNewestFirst(assetId, Limit.of(1)).stream().findFirst()
                .orElseThrow(RecordNotFoundException::new);
        return detail(asset, version);
    }
    public DigitalAssetDto asset(long id, Long actor) { return mapper.toDto(readableAsset(id, actor)); }
    private DigitalAsset readableAsset(long id, Long actor) {
        DigitalAsset asset = requireAsset(id);
        if (!readsPrivate(asset, actor) && (asset.getVisibility() != AssetVisibility.PUBLIC
                || versions.sealedNewestFirst(id, Limit.of(1)).isEmpty()))
            throw new RecordNotFoundException();
        return asset;
    }
    public List<DigitalAssetDto> list(Long actor, String term, String type, boolean archived, int page, int size) {
        int offset = offset(page, size);
        return management.search(actor, term == null ? "" : term, type == null ? "" : type,
                actor != null && archived, offset, size).stream()
                .map(id -> mapper.toDto(requireAsset(id))).toList();
    }
    public List<DigitalAssetVersionDto> history(long id, Long actor, int page, int size) {
        int offset = offset(page, size);
        DigitalAsset asset = readableAsset(id, actor);
        return management.history(id, actor != null && manages(asset, actor), offset, size)
                .stream().map(mapper::toDto).toList();
    }
    @Transactional
    public DigitalAssetDto update(long id, UpdateAssetRequest request, long actor) {
        DigitalAsset asset = writable(id, actor, false);
        if (!Objects.equals(asset.getRowVersion(), request.rowVersion())) throw new RecordConflictException("Reload the asset before editing");
        requireType(request.assetType());
        asset.setTitle(request.title()); asset.setDescription(request.description()); asset.setAssetType(request.assetType());
        return mapper.toDto(management.update(asset));
    }
    @Transactional
    public DigitalAssetDto visibility(long id, AssetVisibility visibility, long actor) {
        DigitalAsset asset = owned(id, actor);
        asset.setVisibility(Objects.requireNonNull(visibility));
        DigitalAsset result = management.update(asset);
        management.flush();
        return mapper.toDto(result);
    }
    @Transactional
    public DigitalAssetDto archive(long id, long actor, boolean archived) {
        DigitalAsset asset = writable(id, actor, false);
        asset.setArchivedAt(archived ? OffsetDateTime.now() : null);
        return mapper.toDto(management.update(asset));
    }
    @Transactional
    public void grant(long id, long account, AssetPermission permission, long actor) {
        owned(id, actor);
        if (!management.activeAccount(account)) throw new IllegalArgumentException("An active account is required");
        management.grant(id, account, permission);
    }
    @Transactional public void revoke(long id, long account, long actor) { owned(id, actor); management.revoke(id, account); }
    @Transactional public List<AssetGrantDto> grants(long id, long actor) { owned(id, actor); return management.grants(id); }
    @Transactional public List<AssetAuditDto> audit(long id, long actor, int page, int size) {
        writable(id, actor, false); return management.audit(id, offset(page,size), size);
    }
    @Transactional
    public DigitalAssetDto transfer(long id, AssetOwnerRequest request, long actor, boolean administrator) {
        request.validate();
        DigitalAsset asset = management.lock(id).orElseThrow(RecordNotFoundException::new);
        if (!owns(asset,actor) && !(administrator && asset.getOwnerAccountId()==null && asset.getOwnerPartyId()==null))
            throw new RecordAccessDeniedException();
        if (request.ownerAccountId()!=null && !management.activeAccount(request.ownerAccountId()))
            throw new IllegalArgumentException("An active account is required");
        if (request.ownerPartyId()!=null) requirePartyManager(request.ownerPartyId(),actor);
        management.actor(actor);
        asset.setOwnerAccountId(request.ownerAccountId()); asset.setOwnerPartyId(request.ownerPartyId());
        management.clearGrants(id);
        return mapper.toDto(management.update(asset));
    }
    @Transactional public void deleteDraft(long versionId,long actor) {
        DigitalAssetVersion version=requireVersion(versionId);
        writable(version.getAssetId(),actor,false); requireUnsealed(version); management.deleteDraft(versionId);
    }
    @Transactional public void deleteEmpty(long id,long actor) { owned(id,actor); management.deleteEmpty(id); }
    public void requirePartyManager(long party,long actor) {
        if(!management.partyManager(party,actor)) throw new RecordAccessDeniedException();
    }
    /** Consumer writes use this inside their transaction to serialize with access changes. */
    @Transactional public DigitalAssetDetailDto link(long id,long actor) {
        DigitalAsset asset=management.lock(id).orElseThrow(RecordNotFoundException::new);
        if(asset.getArchivedAt()!=null) throw new IllegalArgumentException("Cannot attach an archived asset");
        return current(id,actor);
    }
    public DigitalAssetVersion requireSealed(long id) {
        DigitalAssetVersion version=requireVersion(id);
        if(version.getSealedAt()==null) throw new IllegalArgumentException("Link a sealed asset version");
        return version;
    }
    public static int offset(int page,int size) {
        if(page<0 || size<1 || size>100) throw new IllegalArgumentException("Use page >= 0 and size between 1 and 100");
        try { return Math.multiplyExact(page,size); } catch(ArithmeticException e) { throw new IllegalArgumentException("Page is too large"); }
    }
    private void requireType(String code) {
        if(types.findById(code).filter(t->Boolean.TRUE.equals(t.getActive())).isEmpty())
            throw new IllegalArgumentException("Choose an active asset type");
    }
    private DigitalAssetDetailDto detail(DigitalAsset asset,DigitalAssetVersion version) {
        return new DigitalAssetDetailDto(mapper.toDto(asset),mapper.toDto(version),
                files.forVersion(version.getId()).stream().map(mapper::toDto).toList());
    }
    private DigitalAsset requireAsset(long id) { return assets.findById(id).orElseThrow(RecordNotFoundException::new); }
    private DigitalAssetVersion requireVersion(long id) { return versions.findById(id).orElseThrow(RecordNotFoundException::new); }
    private void requireUnsealed(DigitalAssetVersion version) {
        if(version.getSealedAt()!=null) throw new IllegalArgumentException("Sealed assets require a new version");
    }
}
