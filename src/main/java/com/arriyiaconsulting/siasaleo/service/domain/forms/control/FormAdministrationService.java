package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.forms.repository.FormStore;
import com.arriyiaconsulting.siasaleo.service.domain.forms.repository.FormStore.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.transaction.Transactional;
import java.util.*;

/**
 * Authoring lifecycle: define a form, draft versions, preview, publish and
 * retire. Every draft save and the publish step recompile the source, so a
 * stored version always matches what the compiler accepts. Publishing
 * freezes the version (V59 enforces it); changes need another version.
 */
@ApplicationScoped
public class FormAdministrationService {
    @Inject private FormStore store;
    @Inject private FormCompiler compiler;
    @Inject private FormValidator validator;

    @Transactional
    public FormDefinitionDto create(CreateFormDefinitionRequest request, long actor) {
        if ((request.tenantType() == null) != (request.tenantRef() == null))
            throw new IllegalArgumentException("Give both tenant_type and tenant_ref, or neither for a platform core");
        Long tenant = request.tenantType() == null ? null : store.tenant(request.tenantType(), request.tenantRef());
        long id = store.insertDefinition(request.code(), request.purposeKey(), tenant, request.title(), actor);
        return dto(store.definition(id).orElseThrow());
    }
    public List<FormDefinitionDto> definitions(String purpose) { return store.definitions(purpose).stream().map(FormAdministrationService::dto).toList(); }
    public FormDefinitionDto definition(long id) { return dto(requireDefinition(id)); }
    public List<FormVersionDto> versions(long definition) {
        requireDefinition(definition);
        return store.versions(definition).stream().map(FormAdministrationService::dto).toList();
    }
    public FormVersionDto version(long id) { return dto(requireVersion(id)); }

    @Transactional
    public FormVersionDto draft(long definitionId, CreateFormVersionRequest request, long actor) {
        Definition definition = requireDefinition(definitionId);
        JsonObject resolved = compile(definition, request.baseVersionId(), request.source());
        return dto(store.version(store.insertVersion(definitionId, request.baseVersionId(), request.source(), resolved, actor)).orElseThrow());
    }
    @Transactional
    public FormVersionDto revise(long id, UpdateFormVersionRequest request) {
        Version version = requireDraft(id);
        JsonObject resolved = compile(requireDefinition(version.definitionId()), version.baseVersionId(), request.source());
        if (!store.updateDraft(id, request.rowVersion(), request.source(), resolved))
            throw new RecordConflictException("Reload the draft before editing it");
        return version(id);
    }
    @Transactional
    public FormVersionDto publish(long id, long rowVersion, long actor) {
        Version version = requireDraft(id);
        // Recompile: the base core may have been retired since the draft was saved.
        JsonObject resolved = compile(requireDefinition(version.definitionId()), version.baseVersionId(), version.source());
        if (!store.publish(id, rowVersion, resolved, actor)) throw new RecordConflictException("Reload the draft before publishing it");
        return version(id);
    }
    @Transactional
    public FormVersionDto retire(long id, long rowVersion) {
        Version version = requireVersion(id);
        if (!"PUBLISHED".equals(version.status())) throw new IllegalArgumentException("Only published versions can be retired");
        if (!store.retire(id, rowVersion)) throw new RecordConflictException("Reload the version before retiring it");
        return version(id);
    }

    /** Validates sample answers against a version, draft or published, without storing them. */
    public FormPreviewDto preview(long id, PreviewFormRequest request) {
        Version version = requireVersion(id);
        Set<String> accepted = request.acceptedDeclarations() == null ? Set.of() : new HashSet<>(request.acceptedDeclarations());
        var result = validator.validate(version.resolved(), request.answers(),
                request.submit() ? FormValidator.Mode.SUBMIT : FormValidator.Mode.DRAFT, accepted);
        return new FormPreviewDto(result.valid(), result.answers(), result.problems());
    }

    private JsonObject compile(Definition definition, Long baseId, JsonObject source) {
        if (definition.tenantId() == null) {
            if (baseId != null) throw new IllegalArgumentException("A platform core has no base version");
            return compiler.core(source);
        }
        if (baseId == null) throw new IllegalArgumentException("A tenant form extends a published core version; give base_version_id");
        Version base = store.version(baseId).orElseThrow(() -> new IllegalArgumentException("Unknown base version " + baseId));
        Definition core = requireDefinition(base.definitionId());
        if (core.tenantId() != null || !core.purposeKey().equals(definition.purposeKey()))
            throw new IllegalArgumentException("The base must be a platform core with purpose " + definition.purposeKey());
        if (!"PUBLISHED".equals(base.status())) throw new IllegalArgumentException("Extend a published core version");
        return compiler.extend(base.resolved(), source);
    }
    private Definition requireDefinition(long id) { return store.definition(id).orElseThrow(RecordNotFoundException::new); }
    private Version requireVersion(long id) { return store.version(id).orElseThrow(RecordNotFoundException::new); }
    private Version requireDraft(long id) {
        Version version = requireVersion(id);
        if (!"DRAFT".equals(version.status())) throw new IllegalArgumentException("Published versions are frozen; create another version");
        return version;
    }
    static FormDefinitionDto dto(Definition d) {
        return new FormDefinitionDto(d.id(), d.code(), d.purposeKey(), d.tenantId(), d.title(), d.createdAt());
    }
    static FormVersionDto dto(Version v) {
        return new FormVersionDto(v.id(), v.definitionId(), v.number(), v.baseVersionId(), v.status(), v.checksum(), v.rowVersion(),
                v.createdAt(), v.publishedAt(), v.source(), v.resolved());
    }
}
