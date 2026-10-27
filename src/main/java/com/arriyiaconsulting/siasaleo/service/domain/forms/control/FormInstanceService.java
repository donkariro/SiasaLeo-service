package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import com.arriyiaconsulting.siasaleo.service.domain.digitalasset.control.DigitalAssetService;
import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.forms.repository.FormStore;
import com.arriyiaconsulting.siasaleo.service.domain.forms.repository.FormStore.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.transaction.Transactional;
import java.util.*;

/**
 * The API consuming domains use to fill in forms. It performs no
 * authorization of its own: a consumer such as nominations decides who may
 * read, save or submit through its own business record, then calls here.
 * Forms checks structure; the consumer owns every business decision that
 * follows from the answers.
 */
@ApplicationScoped
public class FormInstanceService {
    @Inject private FormStore store;
    @Inject private FormValidator validator;
    @Inject private DigitalAssetService assets;

    /** A published version as seen by a consumer choosing which form to bind. */
    public record PublishedForm(long versionId, String purposeKey, Long tenantId) {}

    public long tenant(String type, String reference) { return store.tenant(type, reference); }

    public PublishedForm published(long versionId) {
        Version version = store.version(versionId).filter(v -> "PUBLISHED".equals(v.status()))
                .orElseThrow(() -> new IllegalArgumentException("Form version " + versionId + " is not published"));
        Definition definition = store.definition(version.definitionId()).orElseThrow();
        return new PublishedForm(versionId, definition.purposeKey(), definition.tenantId());
    }

    @Transactional
    public long open(long versionId, Long tenant, long actor) { return store.insertInstance(versionId, tenant, actor); }

    public FormInstanceDto read(long instanceId) {
        Instance instance = store.instance(instanceId).orElseThrow(RecordNotFoundException::new);
        return dto(instance, store.version(instance.versionId()).orElseThrow());
    }

    /** Saves a draft: answers must have the right shape, but may be incomplete. */
    @Transactional
    public FormInstanceDto save(long instanceId, long rowVersion, JsonObject answers) {
        Instance instance = current(instanceId, rowVersion, "Reload the form before saving");
        Version version = store.version(instance.versionId()).orElseThrow();
        var result = validator.validate(version.resolved(), answers, FormValidator.Mode.DRAFT, Set.of());
        if (!result.valid()) throw new InvalidFormException("Some answers are invalid", result.problems());
        store.saveAnswers(instanceId, rowVersion, result.answers());
        return read(instanceId);
    }

    /**
     * Submits the saved draft as the next immutable revision. rowVersion is
     * the draft the applicant reviewed; repeating a request for the same
     * draft returns the revision it already produced.
     */
    @Transactional
    public FormSubmissionDto submit(long instanceId, long rowVersion, Set<String> accepted, JsonObject context, long actor) {
        Instance instance = store.lockInstance(instanceId).orElseThrow(RecordNotFoundException::new);
        Optional<Submission> repeated = store.submittedAt(instanceId, rowVersion);
        if (repeated.isPresent()) return dto(repeated.get());
        if (instance.rowVersion() != rowVersion) throw new RecordConflictException("Reload the form before submitting it");
        Version version = store.version(instance.versionId()).orElseThrow();
        var result = validator.validate(version.resolved(), instance.answers(), FormValidator.Mode.SUBMIT, accepted);
        List<FormProblem> problems = new ArrayList<>(result.problems());
        result.documents().forEach((field, ids) -> ids.stream().filter(id -> !shareable(id, actor))
                .forEach(id -> problems.add(new FormProblem(field, "Document version " + id + " is not a sealed document you can submit"))));
        if (!problems.isEmpty()) throw new InvalidFormException("The form cannot be submitted yet", problems);

        long id = store.insertSubmission(instance, version, result.answers(), context == null ? JsonValue.EMPTY_JSON_OBJECT : context, actor);
        result.documents().forEach((field, ids) -> ids.forEach(asset -> store.insertEvidence(id, field, asset)));
        for (JsonValue d : version.resolved().getJsonArray("declarations"))
            store.insertDeclaration(id, d.asJsonObject().getString("key"), d.asJsonObject().getString("text"));
        return dto(store.submission(id).orElseThrow());
    }

    public List<FormSubmissionSummaryDto> submissions(long instanceId) {
        return store.submissions(instanceId).stream()
                .map(s -> new FormSubmissionSummaryDto(s.id(), s.revision(), s.submittedBy(), s.submittedAt())).toList();
    }
    public FormSubmissionDto submission(long instanceId, int revision) {
        return dto(store.submission(instanceId, revision).orElseThrow(RecordNotFoundException::new));
    }

    /** The submitter must be able to read a sealed version of the document; submitting shares it, it never grants access. */
    private boolean shareable(long assetVersion, long actor) {
        try { return assets.read(assetVersion, actor).version().sealedAt() != null; }
        catch (RecordNotFoundException e) { return false; }
    }
    private Instance current(long instanceId, long rowVersion, String stale) {
        Instance instance = store.lockInstance(instanceId).orElseThrow(RecordNotFoundException::new);
        if (instance.rowVersion() != rowVersion) throw new RecordConflictException(stale);
        return instance;
    }
    private FormSubmissionDto dto(Submission s) {
        return new FormSubmissionDto(s.id(), s.revision(), s.versionId(), s.checksum(), s.answers(), s.context(),
                store.evidence(s.id()), store.declarations(s.id()), s.submittedBy(), s.submittedAt());
    }
    private static FormInstanceDto dto(Instance i, Version v) {
        return new FormInstanceDto(i.id(), v.id(), v.checksum(), v.resolved(), i.answers(), i.rowVersion(), i.updatedAt());
    }
}
