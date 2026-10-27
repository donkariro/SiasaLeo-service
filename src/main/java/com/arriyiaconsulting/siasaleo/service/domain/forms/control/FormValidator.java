package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.FormProblem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Checks an answer document {"core":{...},"ext":{...}} against a resolved
 * definition. Fields are visited in form order, so conditions see answers that
 * have already been checked. Answers to fields that are not applicable are
 * dropped from the result: hiding a control in the browser is not enough.
 * Drafts are checked for shape only; submissions also need required answers
 * and every declaration accepted.
 */
@ApplicationScoped
public class FormValidator {
    public enum Mode { DRAFT, SUBMIT }

    /** documents maps each document field to the asset version IDs it references. */
    public record Result(JsonObject answers, List<FormProblem> problems, Map<String, List<Long>> documents) {
        public boolean valid() { return problems.isEmpty(); }
    }

    public Result validate(JsonObject definition, JsonObject answers, Mode mode, Set<String> accepted) {
        List<FormProblem> problems = new ArrayList<>();
        Map<String, JsonValue> submitted = flatten(answers == null ? JsonValue.EMPTY_JSON_OBJECT : answers, problems);
        Map<String, JsonValue> clean = new LinkedHashMap<>();
        Map<String, List<Long>> documents = new LinkedHashMap<>();
        Set<String> known = new HashSet<>();
        for (JsonValue s : definition.getJsonArray("sections")) {
            JsonObject section = s.asJsonObject();
            boolean sectionApplies = applies(section, "visibleWhen", clean, true);
            for (JsonValue f : section.getJsonArray("fields")) {
                JsonObject field = f.asJsonObject();
                String key = field.getString("key");
                known.add(key);
                if (!sectionApplies || !applies(field, "visibleWhen", clean, true)) continue;
                JsonValue value = blank(submitted.get(key)) ? null : submitted.get(key);
                if (value == null) {
                    boolean required = FormJson.flag(field, "required") || applies(field, "requiredWhen", clean, false);
                    if (required && mode == Mode.SUBMIT) problems.add(new FormProblem(key, "An answer is required"));
                    continue;
                }
                String error = check(field, value);
                if (error != null) { problems.add(new FormProblem(key, error)); continue; }
                clean.put(key, value);
                if ("document".equals(field.getString("type"))) documents.put(key, ids(value));
            }
        }
        submitted.keySet().stream().filter(k -> !known.contains(k)).sorted()
                .forEach(k -> problems.add(new FormProblem(k, "This form has no such field")));
        if (mode == Mode.SUBMIT) declarations(definition, accepted == null ? Set.of() : accepted, problems);
        return new Result(nest(clean), problems, documents);
    }

    private static void declarations(JsonObject definition, Set<String> accepted, List<FormProblem> problems) {
        Set<String> declared = new HashSet<>();
        for (JsonValue d : definition.getJsonArray("declarations")) {
            String key = d.asJsonObject().getString("key");
            declared.add(key);
            if (!accepted.contains(key)) problems.add(new FormProblem(key, "Accept this declaration to submit"));
        }
        accepted.stream().filter(k -> !declared.contains(k)).sorted()
                .forEach(k -> problems.add(new FormProblem(k, "This form has no such declaration")));
    }

    private static boolean applies(JsonObject element, String name, Map<String, JsonValue> clean, boolean otherwise) {
        return element.get(name) instanceof JsonObject condition ? Conditions.holds(condition, clean) : otherwise;
    }

    private static String check(JsonObject field, JsonValue value) {
        return switch (field.getString("type")) {
            case "text", "textarea" -> {
                if (!(value instanceof JsonString text)) yield "Expected text";
                int length = text.getString().length();
                if (field.containsKey("minLength") && length < field.getInt("minLength")) yield "Use at least " + field.getInt("minLength") + " characters";
                yield length > field.getInt("maxLength") ? "Use at most " + field.getInt("maxLength") + " characters" : null;
            }
            case "number" -> {
                if (!(value instanceof JsonNumber number)) yield "Expected a number";
                if (FormJson.flag(field, "integer") && !number.isIntegral()) yield "Expected a whole number";
                BigDecimal n = number.bigDecimalValue();
                if (field.containsKey("min") && n.compareTo(field.getJsonNumber("min").bigDecimalValue()) < 0) yield "The minimum is " + field.get("min");
                yield field.containsKey("max") && n.compareTo(field.getJsonNumber("max").bigDecimalValue()) > 0 ? "The maximum is " + field.get("max") : null;
            }
            case "date" -> {
                LocalDate date;
                try { date = LocalDate.parse(((JsonString) value).getString()); }
                catch (ClassCastException | DateTimeParseException e) { yield "Expected an ISO date (yyyy-mm-dd)"; }
                if (field.containsKey("min") && date.isBefore(LocalDate.parse(field.getString("min")))) yield "The earliest date is " + field.getString("min");
                yield field.containsKey("max") && date.isAfter(LocalDate.parse(field.getString("max"))) ? "The latest date is " + field.getString("max") : null;
            }
            case "boolean" -> value == JsonValue.TRUE || value == JsonValue.FALSE ? null : "Expected true or false";
            case "choice" -> value instanceof JsonString choice && Conditions.options(field).contains(choice.getString()) ? null : "Choose one of the options";
            case "multichoice" -> {
                Set<String> options = Conditions.options(field);
                if (!(value instanceof JsonArray chosen) || !chosen.stream().allMatch(v -> v instanceof JsonString c && options.contains(c.getString())))
                    yield "Choose from the options";
                if (new HashSet<>(chosen).size() != chosen.size()) yield "Choose each option once";
                if (field.containsKey("minItems") && chosen.size() < field.getInt("minItems")) yield "Choose at least " + field.getInt("minItems");
                yield field.containsKey("maxItems") && chosen.size() > field.getInt("maxItems") ? "Choose at most " + field.getInt("maxItems") : null;
            }
            case "document" -> {
                List<JsonValue> items = value instanceof JsonArray array ? array : List.of(value);
                if (value instanceof JsonArray && !FormJson.flag(field, "multiple")) yield "Attach one document";
                boolean ids = items.stream().allMatch(v -> v instanceof JsonNumber n && n.isIntegral() && n.longValue() > 0);
                if (!ids) yield "Reference documents by asset version ID";
                yield new HashSet<>(ids(value)).size() == items.size() ? null : "Attach each document once";
            }
            default -> "Unsupported field type";
        };
    }

    private static List<Long> ids(JsonValue value) {
        List<JsonValue> items = value instanceof JsonArray array ? array : List.of(value);
        return items.stream().map(v -> ((JsonNumber) v).longValue()).toList();
    }

    private static boolean blank(JsonValue value) {
        return value == null || value == JsonValue.NULL
                || (value instanceof JsonString text && text.getString().isBlank())
                || (value instanceof JsonArray array && array.isEmpty());
    }

    private static Map<String, JsonValue> flatten(JsonObject answers, List<FormProblem> problems) {
        Map<String, JsonValue> flat = new LinkedHashMap<>();
        answers.forEach((group, values) -> {
            if (!FormCompiler.CORE.equals(group) && !FormCompiler.EXTENSION.equals(group))
                problems.add(new FormProblem(group, "Answers are grouped under core and ext"));
            else if (!(values instanceof JsonObject object)) problems.add(new FormProblem(group, "Expected an object of answers"));
            else object.forEach((key, value) -> flat.put(group + "." + key, value));
        });
        return flat;
    }

    private static JsonObject nest(Map<String, JsonValue> clean) {
        Map<String, JsonObjectBuilder> groups = new LinkedHashMap<>();
        clean.forEach((key, value) -> {
            int dot = key.indexOf('.');
            groups.computeIfAbsent(key.substring(0, dot), g -> Json.createObjectBuilder()).add(key.substring(dot + 1), value);
        });
        JsonObjectBuilder out = Json.createObjectBuilder();
        groups.forEach(out::add);
        return out.build();
    }
}
