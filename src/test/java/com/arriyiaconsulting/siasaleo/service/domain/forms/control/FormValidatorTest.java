package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import com.arriyiaconsulting.siasaleo.service.domain.forms.control.FormValidator.Mode;
import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.FormProblem;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.arriyiaconsulting.siasaleo.service.domain.forms.control.FormSamples.*;
import static org.junit.jupiter.api.Assertions.*;

class FormValidatorTest {
    private final FormValidator validator = new FormValidator();
    private static final Set<String> ALL = Set.of("core.truth", "ext.code");

    @Test void completeApplicationIsAccepted() {
        var result = validator.validate(party(), json("""
            {'core':{'phone':'+254700000000','publicOfficer':true,'resignation':41},
             'ext':{'disciplinary':false,'wards':['a','b'],'officerDetail':'Health'}}
            """), Mode.SUBMIT, ALL);
        assertEquals(List.of(), result.problems());
        assertEquals(Map.of("core.resignation", List.of(41L)), result.documents());
    }

    @Test void draftsMayBeIncompleteButSubmissionsMayNot() {
        var draft = validator.validate(party(), json("{'core':{'phone':'0700'}}"), Mode.DRAFT, Set.of());
        assertTrue(draft.valid());
        var submit = validator.validate(party(), json("{'core':{'phone':'0700'}}"), Mode.SUBMIT, Set.of("core.truth"));
        assertFields(submit.problems(), "core.publicOfficer", "ext.disciplinary", "ext.code");
    }

    @Test void conditionalRequirementFollowsTheAnswer() {
        var result = validator.validate(party(), json("""
            {'core':{'phone':'0700','publicOfficer':true},'ext':{'disciplinary':true}}
            """), Mode.SUBMIT, ALL);
        assertFields(result.problems(), "core.resignation", "ext.details");
    }

    @Test void inapplicableAnswersAreDropped() {
        var result = validator.validate(party(), json("""
            {'core':{'phone':'0700','publicOfficer':false,'resignation':41},
             'ext':{'disciplinary':false,'details':'stale text','officerDetail':'Health'}}
            """), Mode.SUBMIT, ALL);
        assertTrue(result.valid(), result.problems()::toString);
        assertEquals(json("{'core':{'phone':'0700','publicOfficer':false},'ext':{'disciplinary':false}}"), result.answers());
        assertTrue(result.documents().isEmpty(), "a hidden document is not submitted");
    }

    @Test void typesAndLimitsAreChecked() {
        var result = validator.validate(party(), json("""
            {'core':{'phone':'012345678901234567890','publicOfficer':'yes'},
             'ext':{'wards':['a','a'],'disciplinary':true,'details':5}}
            """), Mode.DRAFT, Set.of());
        assertFields(result.problems(), "core.phone", "core.publicOfficer", "ext.wards", "ext.details");
        assertEquals(json("{'ext':{'disciplinary':true}}"), result.answers());
    }

    @Test void unansweredYesNoIsNotFalse() {
        var result = validator.validate(party(), json("{'core':{'phone':'0700','publicOfficer':null}}"), Mode.SUBMIT, ALL);
        assertTrue(result.problems().stream().anyMatch(p -> p.field().equals("core.publicOfficer")));
    }

    @Test void unknownFieldsGroupsAndDeclarationsAreRejected() {
        var result = validator.validate(party(), json("""
            {'core':{'phone':'0700','nationalId':'1'},'extra':{}}
            """), Mode.SUBMIT, Set.of("core.truth", "ext.code", "ext.other"));
        assertTrue(result.problems().contains(new FormProblem("core.nationalId", "This form has no such field")));
        assertTrue(result.problems().contains(new FormProblem("extra", "Answers are grouped under core and ext")));
        assertTrue(result.problems().contains(new FormProblem("ext.other", "This form has no such declaration")));
    }

    @Test void documentReferencesArePositiveIds() {
        var result = validator.validate(party(), json("{'core':{'publicOfficer':true,'resignation':[1,2]}}"), Mode.DRAFT, Set.of());
        assertFields(result.problems(), "core.resignation");
        result = validator.validate(party(), json("{'core':{'publicOfficer':true,'resignation':-3}}"), Mode.DRAFT, Set.of());
        assertFields(result.problems(), "core.resignation");
    }

    private static void assertFields(List<FormProblem> problems, String... fields) {
        assertEquals(new TreeSet<>(List.of(fields)), new TreeSet<>(problems.stream().map(FormProblem::field).toList()), problems::toString);
    }
}
