package com.arriyiaconsulting.siasaleo.service.domain.nomination.repository;

import com.arriyiaconsulting.siasaleo.service.domain.nomination.entity.NominationFormPurpose;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence.NativeQueries;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;

import static com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping.SqlValues.*;

/** Authority lookups, cross-domain existence checks and the links between nomination records and forms. */
@ApplicationScoped
public class NominationRecords {
    @Inject private NativeQueries sql;

    public boolean manager(long party, long account) {
        return sql.exists("SELECT count(*) FROM nomination_manager m JOIN user_account u ON u.id=m.account_id "
                + "WHERE m.political_party_id=?1 AND m.account_id=?2 AND u.status='ACTIVE'", party, account);
    }
    public void manager(long party, long account, long actor, boolean remove) {
        if (remove) sql.execute("DELETE FROM nomination_manager WHERE political_party_id=?1 AND account_id=?2", party, account);
        else sql.execute("INSERT INTO nomination_manager(political_party_id,account_id,granted_by_account_id) VALUES(?1,?2,?3) "
                + "ON CONFLICT DO NOTHING", party, account, actor);
    }
    public boolean activeAccount(long account) { return sql.exists("SELECT count(*) FROM user_account WHERE id=?1 AND status='ACTIVE'", account); }
    public Optional<Long> personOf(long account) {
        return sql.rows("SELECT person_id,id FROM user_account WHERE id=?1 AND status='ACTIVE' AND person_id IS NOT NULL", account)
                .stream().map(r -> number(r[0])).findFirst();
    }
    /** The start date of the person's current membership of the party, if any. */
    public Optional<String> membershipStart(long person, long party) {
        return sql.rows("SELECT CAST(start_date AS text),id FROM party_membership WHERE person_id=?1 AND political_party_id=?2 AND end_date IS NULL",
                person, party).stream().map(r -> text(r[0])).findFirst();
    }
    public boolean partyExists(long party) { return sql.exists("SELECT count(*) FROM political_party WHERE id=?1", party); }
    public boolean eventExists(long event) { return sql.exists("SELECT count(*) FROM election_event WHERE id=?1", event); }
    public boolean contestInEvent(long contest, long event) {
        return sql.exists("SELECT count(*) FROM contest WHERE id=?1 AND election_event_id=?2", contest, event);
    }

    public Map<NominationFormPurpose, Long> exerciseForms(long exercise) {
        Map<NominationFormPurpose, Long> forms = new EnumMap<>(NominationFormPurpose.class);
        sql.rows("SELECT purpose_code,form_version_id FROM nomination_exercise_form WHERE exercise_id=?1", exercise)
                .forEach(r -> forms.put(NominationFormPurpose.valueOf((String) r[0]), number(r[1])));
        return forms;
    }
    public void bindForm(long exercise, NominationFormPurpose purpose, long version) {
        sql.execute("INSERT INTO nomination_exercise_form(exercise_id,purpose_code,form_version_id) VALUES(?1,?2,?3) "
                + "ON CONFLICT(exercise_id,purpose_code) DO UPDATE SET form_version_id=EXCLUDED.form_version_id", exercise, purpose.name(), version);
    }
    public Optional<Long> applicationForm(long application, NominationFormPurpose purpose) {
        return sql.rows("SELECT form_instance_id,application_id FROM nomination_application_form WHERE application_id=?1 AND purpose_code=?2",
                application, purpose.name()).stream().map(r -> number(r[0])).findFirst();
    }
    public void linkForm(long application, NominationFormPurpose purpose, long instance) {
        sql.execute("INSERT INTO nomination_application_form(application_id,purpose_code,form_instance_id) VALUES(?1,?2,?3)",
                application, purpose.name(), instance);
    }
}
