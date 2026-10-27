package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.FormProblem;
import jakarta.json.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.arriyiaconsulting.siasaleo.service.domain.forms.control.FormSamples.*;
import static org.junit.jupiter.api.Assertions.*;

class FormCompilerTest {
    private final FormCompiler compiler = new FormCompiler();

    @Test void coreKeysAndConditionsAreQualified() {
        JsonObject field = fields(core(), 0).get(2);
        assertEquals("core.resignation", field.getString("key"));
        assertEquals("core.publicOfficer", field.getJsonObject("visibleWhen").getString("field"));
        assertEquals("core.truth", core().getJsonArray("declarations").getJsonObject(0).getString("key"));
        assertEquals(20, fields(core(), 0).get(0).getInt("maxLength"));
        assertEquals(255, fields(compiler.core(json("""
            {'format':1,'title':'T','sections':[{'key':'s','title':'S','fields':[{'key':'a','type':'text','label':'A'}]}]}
            """)), 0).get(0).getInt("maxLength"), "text defaults to 255 characters");
    }

    @Test void extensionAddsWithinPolicyAndKeepsCore() {
        JsonObject party = party();
        assertFalse(party.containsKey("extensionPolicy"), "extensions cannot be extended again");
        assertEquals("Use a number the nomination office can reach.", fields(party, 0).get(0).getString("help"));
        List<JsonObject> added = fields(party, 1);
        assertEquals(List.of("ext.disciplinary", "ext.details", "ext.wards", "ext.officerDetail"),
                added.stream().map(f -> f.getString("key")).toList());
        assertEquals("core.publicOfficer", added.get(3).getJsonObject("visibleWhen").getString("field"));
        assertEquals(2, party.getJsonArray("declarations").size());
    }

    @Test void extensionCannotGoBeyondPolicy() {
        InvalidFormException e = assertThrows(InvalidFormException.class, () -> compiler.extend(core(), json("""
            {'format':1,'help':{'publicOfficer':'x'},
             'addFields':[{'section':'contact','fields':[{'key':'x','type':'text','label':'X'}]}],
             'addSections':[{'key':'s','title':'S','fields':[]}]}
            """)));
        assertProblem(e, "publicOfficer", "does not allow help text");
        assertProblem(e, "contact", "does not allow additional fields");
        assertProblem(e, "addSections", "does not allow additional sections");
    }

    @Test void extensionKeysCannotImpersonateCoreFields() {
        InvalidFormException e = assertThrows(InvalidFormException.class, () -> compiler.extend(core(), json("""
            {'format':1,'addFields':[{'section':'additional','fields':[{'key':'core.phone','type':'text','label':'X'}]}]}
            """)));
        assertProblem(e, "core.phone", "Field keys are letters");
    }

    @Test void conditionsMayOnlyLookBackwards() {
        InvalidFormException e = assertThrows(InvalidFormException.class, () -> compiler.core(json("""
            {'format':1,'title':'T','sections':[{'key':'s','title':'S','fields':[
              {'key':'a','type':'text','label':'A','visibleWhen':{'field':'b','equals':true}},
              {'key':'b','type':'boolean','label':'B'}]}]}
            """)));
        assertProblem(e, "core.a", "unknown or later field");
    }

    @Test void operatorsMustFitTheFieldType() {
        InvalidFormException e = assertThrows(InvalidFormException.class, () -> compiler.core(json("""
            {'format':1,'title':'T','sections':[{'key':'s','title':'S','fields':[
              {'key':'c','type':'choice','label':'C','options':[{'value':'x','label':'X'}]},
              {'key':'a','type':'text','label':'A','visibleWhen':{'field':'c','equals':'y'}},
              {'key':'b','type':'text','label':'B','visibleWhen':{'field':'c','contains':'x'}}]}]}
            """)));
        assertProblem(e, "core.a", "'equals' does not fit choice");
        assertProblem(e, "core.b", "'contains' does not fit choice");
    }

    @Test void typosAndDuplicatesAreReportedTogether() {
        InvalidFormException e = assertThrows(InvalidFormException.class, () -> compiler.core(json("""
            {'format':2,'title':'T','sections':[{'key':'s','title':'S','fields':[
              {'key':'a','type':'text','label':'A','requred':true},
              {'key':'b','type':'text','label':'B'},
              {'key':'b','type':'text','label':'B2'},
              {'key':'d','type':'signature','label':'D'}]}]}
            """)));
        assertProblem(e, "format", "Set format to 1");
        assertProblem(e, "core.a", "Unknown attribute: requred");
        assertProblem(e, "core.b", "Duplicate field key");
        assertProblem(e, "core.d", "Unsupported field type");
    }

    @Test void policyMustNameRealSectionsAndFields() {
        InvalidFormException e = assertThrows(InvalidFormException.class, () -> compiler.core(json("""
            {'format':1,'title':'T','sections':[{'key':'s','title':'S','fields':[]}],
             'extensionPolicy':{'addFieldsTo':['missing'],'helpEditable':['nope']}}
            """)));
        assertProblem(e, "extensionPolicy", "Unknown section in addFieldsTo: missing");
        assertProblem(e, "extensionPolicy", "Unknown field in helpEditable: nope");
    }

    @Test void compilationIsRepeatable() {
        assertEquals(party(), compiler.extend(core(), json(EXTENSION)));
    }

    static List<JsonObject> fields(JsonObject form, int section) {
        return form.getJsonArray("sections").getJsonObject(section).getJsonArray("fields").getValuesAs(JsonObject.class);
    }
    static void assertProblem(InvalidFormException e, String field, String message) {
        List<FormProblem> problems = e.getProblems();
        assertTrue(problems.stream().anyMatch(p -> java.util.Objects.equals(field, p.field()) && p.message().contains(message)),
                () -> "Expected " + field + ": " + message + " in " + problems);
    }
}
