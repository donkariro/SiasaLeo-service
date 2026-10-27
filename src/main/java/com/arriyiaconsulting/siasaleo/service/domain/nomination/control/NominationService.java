package com.arriyiaconsulting.siasaleo.service.domain.nomination.control;

import com.arriyiaconsulting.siasaleo.service.domain.forms.control.FormInstanceService;
import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.nomination.dto.*;
import com.arriyiaconsulting.siasaleo.service.domain.nomination.entity.*;
import com.arriyiaconsulting.siasaleo.service.domain.nomination.repository.*;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.transaction.Transactional;
import java.time.*;
import java.util.*;

/**
 * A party's nomination exercise and its aspirants' applications
 * (docs/nominations.md). Nominations owns who may act and when; the forms
 * domain owns the questions and answers. Party staff need an explicit
 * nomination_manager assignment: administrators provision managers and author
 * forms, but do not read applications by virtue of their role.
 */
@ApplicationScoped
public class NominationService {
    /** The forms purpose every nomination application form is published under. */
    public static final String FORM_PURPOSE = "nomination.application";
    /** How a political party appears as a forms tenant. */
    public static final String TENANT_TYPE = "POLITICAL_PARTY";

    @Inject private NominationExerciseRepository exercises;
    @Inject private NominationApplicationRepository applications;
    @Inject private NominationRecords records;
    @Inject private FormInstanceService forms;
    private Clock clock = Clock.systemUTC();

    @Transactional
    public void manager(long party, long account, long actor, boolean remove) {
        if (!records.partyExists(party)) throw new RecordNotFoundException();
        if (!remove && !records.activeAccount(account)) throw new IllegalArgumentException("Assign an active account");
        records.manager(party, account, actor, remove);
    }

    @Transactional
    public NominationExerciseDto create(CreateNominationExerciseRequest request, long actor) {
        requireManager(request.politicalPartyId(), actor);
        if (!records.eventExists(request.electionEventId())) throw new IllegalArgumentException("Unknown election event");
        if (!request.closesAt().isAfter(request.opensAt())) throw new IllegalArgumentException("closes_at must be after opens_at");
        return dto(exercises.insert(new NominationExercise(request.politicalPartyId(), request.electionEventId(),
                request.title(), request.opensAt(), request.closesAt(), actor)));
    }
    public NominationExerciseDto exercise(long id, Long actor) {
        NominationExercise exercise = requireExercise(id);
        if (exercise.getStatus() == ExerciseStatus.DRAFT && (actor == null || !records.manager(exercise.getPoliticalPartyId(), actor)))
            throw new RecordNotFoundException();
        return dto(exercise);
    }
    /** Managers see every exercise of their party; everyone else sees exercises that have opened. */
    public List<NominationExerciseDto> exercises(long party, Long actor) {
        boolean manager = actor != null && records.manager(party, actor);
        return exercises.forParty(party).stream().filter(e -> manager || e.getStatus() != ExerciseStatus.DRAFT).map(this::dto).toList();
    }

    /** Chooses the published form an exercise uses: the platform core itself, or this party's extension of it. */
    @Transactional
    public NominationExerciseDto bindForm(long id, NominationFormPurpose purpose, long formVersion, long actor) {
        NominationExercise exercise = managedExercise(id, actor);
        if (exercise.getStatus() != ExerciseStatus.DRAFT) throw new IllegalArgumentException("Forms are fixed once the exercise opens");
        FormInstanceService.PublishedForm form = forms.published(formVersion);
        if (!FORM_PURPOSE.equals(form.purposeKey())) throw new IllegalArgumentException("Choose a " + FORM_PURPOSE + " form");
        if (form.tenantId() != null && form.tenantId() != tenant(exercise))
            throw new IllegalArgumentException("That form belongs to another tenant");
        records.bindForm(id, purpose, formVersion);
        return dto(exercise);
    }
    @Transactional
    public NominationExerciseDto open(long id, long rowVersion, long actor) {
        NominationExercise exercise = managedExercise(id, actor);
        requireCurrent(exercise.getRowVersion(), rowVersion);
        if (!records.exerciseForms(id).containsKey(NominationFormPurpose.INITIAL_APPLICATION))
            throw new IllegalArgumentException("Choose the application form before opening the exercise");
        exercise.open();
        return dto(exercises.update(exercise));
    }
    @Transactional
    public NominationExerciseDto close(long id, long rowVersion, long actor) {
        NominationExercise exercise = managedExercise(id, actor);
        requireCurrent(exercise.getRowVersion(), rowVersion);
        exercise.close();
        return dto(exercises.update(exercise));
    }

    /** Starts the caller's own application. Only a current member of the party may apply. */
    @Transactional
    public NominationApplicationDto apply(long exerciseId, long contestId, long actor) {
        NominationExercise exercise = requireExercise(exerciseId);
        long person = records.personOf(actor).orElseThrow(() -> new IllegalArgumentException("Link your account to a person before applying"));
        requireAccepting(exercise);
        if (records.membershipStart(person, exercise.getPoliticalPartyId()).isEmpty())
            throw new IllegalArgumentException("Only current members of the party can apply");
        if (!records.contestInEvent(contestId, exercise.getElectionEventId()))
            throw new IllegalArgumentException("Choose a contest in this exercise's election event");
        long version = records.exerciseForms(exerciseId).get(NominationFormPurpose.INITIAL_APPLICATION);
        NominationApplication application = applications.insert(new NominationApplication(exerciseId, person, contestId, actor));
        long instance = forms.open(version, tenant(exercise), actor);
        records.linkForm(application.getId(), NominationFormPurpose.INITIAL_APPLICATION, instance);
        return detail(application);
    }
    public NominationApplicationDto application(long id, long actor) {
        NominationApplication application = requireApplication(id);
        if (!applicant(application, actor) && !records.manager(requireExercise(application.getExerciseId()).getPoliticalPartyId(), actor))
            throw new RecordNotFoundException();
        return detail(application);
    }
    public List<NominationApplicationSummaryDto> mine(long actor) {
        return records.personOf(actor).map(p -> applications.forPerson(p).stream().map(NominationService::summary).toList()).orElse(List.of());
    }
    public List<NominationApplicationSummaryDto> applications(long exerciseId, long actor) {
        managedExercise(exerciseId, actor);
        return applications.forExercise(exerciseId).stream().map(NominationService::summary).toList();
    }

    @Transactional
    public NominationApplicationDto save(long id, SaveApplicationAnswersRequest request, long actor) {
        NominationApplication application = ownApplication(id, actor);
        forms.save(instance(application), request.rowVersion(), request.answers());
        return detail(application);
    }
    /** Submits the saved draft as a new revision; resubmitting corrections is allowed until the exercise closes. */
    @Transactional
    public FormSubmissionDto submit(long id, SubmitApplicationRequest request, long actor) {
        NominationApplication application = ownApplication(id, actor);
        NominationExercise exercise = requireExercise(application.getExerciseId());
        Set<String> accepted = request.acceptedDeclarations() == null ? Set.of() : new HashSet<>(request.acceptedDeclarations());
        JsonObjectBuilder context = Json.createObjectBuilder()
                .add("exercise_id", exercise.getId())
                .add("political_party_id", exercise.getPoliticalPartyId())
                .add("election_event_id", exercise.getElectionEventId())
                .add("contest_id", application.getContestId())
                .add("person_id", application.getPersonId());
        records.membershipStart(application.getPersonId(), exercise.getPoliticalPartyId())
                .ifPresentOrElse(start -> context.add("membership_start_date", start),
                        () -> { throw new IllegalArgumentException("Only current members of the party can submit"); });
        FormSubmissionDto submission = forms.submit(instance(application), request.rowVersion(), accepted, context.build(), actor);
        application.submitted(submission.submittedAt());
        applications.update(application);
        return submission;
    }
    public FormSubmissionDto submission(long id, int revision, long actor) {
        application(id, actor); // Same access as the application itself.
        return forms.submission(instance(requireApplication(id)), revision);
    }
    @Transactional
    public NominationApplicationSummaryDto withdraw(long id, long rowVersion, long actor) {
        NominationApplication application = requireApplication(id);
        if (!applicant(application, actor)) throw new RecordNotFoundException();
        requireCurrent(application.getRowVersion(), rowVersion);
        application.withdraw(OffsetDateTime.now(clock));
        return summary(applications.update(application));
    }

    private NominationApplication ownApplication(long id, long actor) {
        NominationApplication application = requireApplication(id);
        if (!applicant(application, actor)) throw new RecordNotFoundException();
        application.requireLive();
        requireAccepting(requireExercise(application.getExerciseId()));
        return application;
    }
    private boolean applicant(NominationApplication application, long actor) {
        return records.personOf(actor).filter(p -> p.equals(application.getPersonId())).isPresent();
    }
    private void requireAccepting(NominationExercise exercise) {
        if (!exercise.accepting(OffsetDateTime.now(clock))) throw new IllegalArgumentException("The exercise is not accepting applications");
    }
    private NominationExercise managedExercise(long id, long actor) {
        NominationExercise exercise = requireExercise(id);
        if (!records.manager(exercise.getPoliticalPartyId(), actor)) throw new RecordAccessDeniedException();
        return exercise;
    }
    private void requireManager(long party, long actor) {
        if (!records.partyExists(party)) throw new IllegalArgumentException("Unknown political party");
        if (!records.manager(party, actor)) throw new RecordAccessDeniedException();
    }
    private static void requireCurrent(Long stored, long expected) {
        if (!Objects.equals(stored, expected)) throw new RecordConflictException("Reload the record before changing it");
    }
    private long tenant(NominationExercise exercise) { return forms.tenant(TENANT_TYPE, exercise.getPoliticalPartyId().toString()); }
    private long instance(NominationApplication application) {
        return records.applicationForm(application.getId(), NominationFormPurpose.INITIAL_APPLICATION).orElseThrow();
    }
    private NominationExercise requireExercise(long id) { return exercises.findById(id).orElseThrow(RecordNotFoundException::new); }
    private NominationApplication requireApplication(long id) { return applications.findById(id).orElseThrow(RecordNotFoundException::new); }

    private NominationApplicationDto detail(NominationApplication application) {
        long instance = instance(application);
        return new NominationApplicationDto(summary(application), forms.read(instance), forms.submissions(instance));
    }
    private NominationExerciseDto dto(NominationExercise e) {
        Map<String, Long> bound = new LinkedHashMap<>();
        if (e.getId() != null) records.exerciseForms(e.getId()).forEach((purpose, version) -> bound.put(purpose.name(), version));
        return new NominationExerciseDto(e.getId(), e.getPoliticalPartyId(), e.getElectionEventId(), e.getTitle(), e.getStatus().name(),
                e.getOpensAt(), e.getClosesAt(), e.getRowVersion(), bound);
    }
    private static NominationApplicationSummaryDto summary(NominationApplication a) {
        return new NominationApplicationSummaryDto(a.getId(), a.getExerciseId(), a.getPersonId(), a.getContestId(), a.getStatus().name(),
                a.getCreatedAt(), a.getSubmittedAt(), a.getWithdrawnAt(), a.getRowVersion());
    }
}
