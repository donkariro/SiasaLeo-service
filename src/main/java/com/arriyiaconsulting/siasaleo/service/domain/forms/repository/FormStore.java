package com.arriyiaconsulting.siasaleo.service.domain.forms.repository;

import com.arriyiaconsulting.siasaleo.service.domain.forms.control.FormJson;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import java.time.OffsetDateTime;
import java.util.*;

import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.SqlValues.*;

/**
 * Native persistence for the forms schema. JSONB goes in as CAST(? AS jsonb)
 * and comes back as text, which keeps the JSON columns independent of JPA
 * provider and driver type mappings. Lifecycle rules live in FormStore's
 * callers and in the V59 triggers; this class only reads and writes rows.
 */
@ApplicationScoped
public class FormStore {
    @Inject private NativeQueries sql;

    public record Definition(long id, String code, String purposeKey, Long tenantId, String title, OffsetDateTime createdAt) {}
    public record Version(long id, long definitionId, int number, Long baseVersionId, String status, JsonObject source,
                          JsonObject resolved, String checksum, long rowVersion, OffsetDateTime createdAt, OffsetDateTime publishedAt) {}
    public record Instance(long id, long versionId, Long tenantId, JsonObject answers, long rowVersion, OffsetDateTime updatedAt) {}
    public record Submission(long id, long instanceId, int revision, long versionId, String checksum, JsonObject answers,
                             JsonObject context, long submittedBy, OffsetDateTime submittedAt) {}

    public long tenant(String type, String reference) {
        return sql.insert("INSERT INTO forms.tenant(tenant_type,external_ref) VALUES(?1,?2) "
                + "ON CONFLICT(tenant_type,external_ref) DO UPDATE SET tenant_type=EXCLUDED.tenant_type RETURNING id", type, reference);
    }

    public long insertDefinition(String code, String purpose, Long tenant, String title, long actor) {
        return sql.insert("INSERT INTO forms.form_definition(code,purpose_key,tenant_id,title,created_by_account_id) "
                + "VALUES(?1,?2,CAST(?3 AS bigint),?4,?5) RETURNING id", code, purpose, tenant, title, actor);
    }
    private static final String DEFINITION = "SELECT id,code,purpose_key,tenant_id,title,created_at FROM forms.form_definition ";
    public Optional<Definition> definition(long id) { return sql.rows(DEFINITION + "WHERE id=?1", id).stream().map(FormStore::definition).findFirst(); }
    public List<Definition> definitions(String purpose) {
        return sql.rows(DEFINITION + "WHERE (?1='' OR purpose_key=?1) ORDER BY purpose_key,tenant_id NULLS FIRST,code", purpose == null ? "" : purpose)
                .stream().map(FormStore::definition).toList();
    }

    /** Locks the definition so version numbers are allocated one writer at a time. */
    public long insertVersion(long definition, Long base, JsonObject source, JsonObject resolved, long actor) {
        sql.rows("SELECT id,id FROM forms.form_definition WHERE id=?1 FOR UPDATE", definition);
        return sql.insert("INSERT INTO forms.form_version(definition_id,version_number,base_version_id,source,resolved,created_by_account_id) "
                + "SELECT ?1,COALESCE(MAX(version_number),0)+1,CAST(?2 AS bigint),CAST(?3 AS jsonb),CAST(?4 AS jsonb),?5 "
                + "FROM forms.form_version WHERE definition_id=?1 RETURNING id",
                definition, base, FormJson.text(source), FormJson.text(resolved), actor);
    }
    private static final String VERSION = "SELECT id,definition_id,version_number,base_version_id,status,CAST(source AS text),"
            + "CAST(resolved AS text),checksum,row_version,created_at,published_at FROM forms.form_version ";
    public Optional<Version> version(long id) { return sql.rows(VERSION + "WHERE id=?1", id).stream().map(FormStore::version).findFirst(); }
    public List<Version> versions(long definition) {
        return sql.rows(VERSION + "WHERE definition_id=?1 ORDER BY version_number DESC", definition).stream().map(FormStore::version).toList();
    }
    /** Compare-and-set on row_version; false means the draft changed or was published meanwhile. */
    public boolean updateDraft(long id, long rowVersion, JsonObject source, JsonObject resolved) {
        return sql.update("UPDATE forms.form_version SET source=CAST(?1 AS jsonb),resolved=CAST(?2 AS jsonb),row_version=row_version+1 "
                + "WHERE id=?3 AND row_version=?4 AND status='DRAFT'", FormJson.text(source), FormJson.text(resolved), id, rowVersion) == 1;
    }
    public boolean publish(long id, long rowVersion, JsonObject resolved, long actor) {
        return sql.update("UPDATE forms.form_version SET status='PUBLISHED',resolved=CAST(?1 AS jsonb),published_by_account_id=?2,"
                + "published_at=CURRENT_TIMESTAMP,row_version=row_version+1 WHERE id=?3 AND row_version=?4 AND status='DRAFT'",
                FormJson.text(resolved), actor, id, rowVersion) == 1;
    }
    public boolean retire(long id, long rowVersion) {
        return sql.update("UPDATE forms.form_version SET status='RETIRED',row_version=row_version+1 "
                + "WHERE id=?1 AND row_version=?2 AND status='PUBLISHED'", id, rowVersion) == 1;
    }

    public long insertInstance(long version, Long tenant, long actor) {
        return sql.insert("INSERT INTO forms.form_instance(form_version_id,tenant_id,created_by_account_id) "
                + "VALUES(?1,CAST(?2 AS bigint),?3) RETURNING id", version, tenant, actor);
    }
    private static final String INSTANCE = "SELECT id,form_version_id,tenant_id,CAST(answers AS text),row_version,updated_at FROM forms.form_instance ";
    public Optional<Instance> instance(long id) { return sql.rows(INSTANCE + "WHERE id=?1", id).stream().map(FormStore::instance).findFirst(); }
    /** Serializes saves and submissions of one instance. */
    public Optional<Instance> lockInstance(long id) { return sql.rows(INSTANCE + "WHERE id=?1 FOR UPDATE", id).stream().map(FormStore::instance).findFirst(); }
    public boolean saveAnswers(long id, long rowVersion, JsonObject answers) {
        return sql.update("UPDATE forms.form_instance SET answers=CAST(?1 AS jsonb),updated_at=CURRENT_TIMESTAMP,row_version=row_version+1 "
                + "WHERE id=?2 AND row_version=?3", FormJson.text(answers), id, rowVersion) == 1;
    }

    public long insertSubmission(Instance instance, Version version, JsonObject answers, JsonObject context, long actor) {
        return sql.insert("INSERT INTO forms.form_submission(instance_id,revision,form_version_id,definition_checksum,answers,context,"
                + "instance_row_version,submitted_by_account_id) SELECT ?1,COALESCE(MAX(revision),0)+1,?2,?3,CAST(?4 AS jsonb),"
                + "CAST(?5 AS jsonb),?6,?7 FROM forms.form_submission WHERE instance_id=?1 RETURNING id",
                instance.id(), version.id(), version.checksum(), FormJson.text(answers), FormJson.text(context), instance.rowVersion(), actor);
    }
    public void insertEvidence(long submission, String field, long assetVersion) {
        sql.execute("INSERT INTO forms.submission_evidence(submission_id,field_key,asset_version_id) VALUES(?1,?2,?3)", submission, field, assetVersion);
    }
    public void insertDeclaration(long submission, String key, String text) {
        sql.execute("INSERT INTO forms.declaration_acceptance(submission_id,declaration_key,declaration_text) VALUES(?1,?2,?3)", submission, key, text);
    }
    private static final String SUBMISSION = "SELECT id,instance_id,revision,form_version_id,definition_checksum,CAST(answers AS text),"
            + "CAST(context AS text),submitted_by_account_id,submitted_at FROM forms.form_submission ";
    public Optional<Submission> submission(long id) { return sql.rows(SUBMISSION + "WHERE id=?1", id).stream().map(FormStore::submission).findFirst(); }
    public Optional<Submission> submission(long instance, int revision) {
        return sql.rows(SUBMISSION + "WHERE instance_id=?1 AND revision=?2", instance, revision).stream().map(FormStore::submission).findFirst();
    }
    public Optional<Submission> submittedAt(long instance, long rowVersion) {
        return sql.rows(SUBMISSION + "WHERE instance_id=?1 AND instance_row_version=?2", instance, rowVersion).stream().map(FormStore::submission).findFirst();
    }
    public List<Submission> submissions(long instance) {
        return sql.rows(SUBMISSION + "WHERE instance_id=?1 ORDER BY revision DESC", instance).stream().map(FormStore::submission).toList();
    }
    public Map<String, List<Long>> evidence(long submission) {
        Map<String, List<Long>> out = new LinkedHashMap<>();
        sql.rows("SELECT field_key,asset_version_id FROM forms.submission_evidence WHERE submission_id=?1 ORDER BY field_key,asset_version_id", submission)
                .forEach(r -> out.computeIfAbsent((String) r[0], k -> new ArrayList<>()).add(number(r[1])));
        return out;
    }
    public Map<String, String> declarations(long submission) {
        Map<String, String> out = new LinkedHashMap<>();
        sql.rows("SELECT declaration_key,declaration_text FROM forms.declaration_acceptance WHERE submission_id=?1 ORDER BY declaration_key", submission)
                .forEach(r -> out.put((String) r[0], (String) r[1]));
        return out;
    }

    private static Definition definition(Object[] r) {
        return new Definition(number(r[0]), text(r[1]), text(r[2]), number(r[3]), text(r[4]), FormJson.time(r[5]));
    }
    private static Version version(Object[] r) {
        return new Version(number(r[0]), number(r[1]), number(r[2]).intValue(), number(r[3]), text(r[4]), FormJson.parse(text(r[5])),
                FormJson.parse(text(r[6])), text(r[7]), number(r[8]), FormJson.time(r[9]), FormJson.time(r[10]));
    }
    private static Instance instance(Object[] r) {
        return new Instance(number(r[0]), number(r[1]), number(r[2]), FormJson.parse(text(r[3])), number(r[4]), FormJson.time(r[5]));
    }
    private static Submission submission(Object[] r) {
        return new Submission(number(r[0]), number(r[1]), number(r[2]).intValue(), number(r[3]), text(r[4]), FormJson.parse(text(r[5])),
                FormJson.parse(text(r[6])), number(r[7]), FormJson.time(r[8]));
    }
}
