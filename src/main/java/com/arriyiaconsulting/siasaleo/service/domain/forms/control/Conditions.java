package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.FormProblem;
import jakarta.json.*;
import java.util.*;

/**
 * The restricted condition language for visibleWhen and requiredWhen:
 * {"field":k,"equals":v}, {"field":k,"in":[...]}, {"field":k,"contains":v}
 * (multichoice), combined with {"all":[...]}, {"any":[...]} and {"not":c}.
 * The Angular renderer implements the same semantics; nothing executes code
 * supplied by a definition.
 */
final class Conditions {
    private static final Set<String> COMPARABLE = Set.of("text", "textarea", "number", "date", "boolean", "choice");
    private static final Set<String> OPERATORS = Set.of("equals", "in", "contains");

    private Conditions() { }

    /**
     * Qualifies field references with the author's origin and checks them
     * against fields that come earlier in the form. Only backward references
     * are allowed, which rules out cycles and lets one ordered pass evaluate
     * every condition. Returns null after recording a problem.
     */
    static JsonObject compile(JsonValue raw, String origin, Map<String, JsonObject> earlier, String where, List<FormProblem> problems) {
        if (!(raw instanceof JsonObject condition)) return fail(problems, where, "A condition is a JSON object");
        for (String op : List.of("all", "any")) {
            if (!condition.containsKey(op)) continue;
            if (condition.size() != 1 || !(condition.get(op) instanceof JsonArray items) || items.isEmpty())
                return fail(problems, where, "'" + op + "' takes a non-empty list of conditions and nothing else");
            JsonArrayBuilder compiled = Json.createArrayBuilder();
            for (JsonValue item : items) {
                JsonObject inner = compile(item, origin, earlier, where, problems);
                if (inner == null) return null;
                compiled.add(inner);
            }
            return Json.createObjectBuilder().add(op, compiled).build();
        }
        if (condition.containsKey("not")) {
            if (condition.size() != 1) return fail(problems, where, "'not' takes one condition and nothing else");
            JsonObject inner = compile(condition.get("not"), origin, earlier, where, problems);
            return inner == null ? null : Json.createObjectBuilder().add("not", inner).build();
        }
        String reference = FormJson.string(condition, "field");
        if (reference == null) return fail(problems, where, "A condition names a field, or combines conditions with all, any or not");
        Set<String> operators = new HashSet<>(condition.keySet());
        operators.remove("field");
        if (operators.size() != 1 || !OPERATORS.containsAll(operators))
            return fail(problems, where, "A field condition uses exactly one of equals, in or contains");
        String operator = operators.iterator().next();
        String key = reference.contains(".") ? reference : origin + "." + reference;
        JsonObject field = earlier.get(key);
        if (field == null) return fail(problems, where, "Condition refers to an unknown or later field: " + reference);
        String type = FormJson.string(field, "type");
        JsonValue value = condition.get(operator);
        boolean valid = switch (operator) {
            case "equals" -> COMPARABLE.contains(type) && matches(type, field, value);
            case "in" -> COMPARABLE.contains(type) && value instanceof JsonArray values && !values.isEmpty()
                    && values.stream().allMatch(v -> matches(type, field, v));
            default -> "multichoice".equals(type) && matches("choice", field, value);
        };
        if (!valid) return fail(problems, where, "'" + operator + "' does not fit " + type + " field " + reference);
        return Json.createObjectBuilder().add("field", key).add(operator, value).build();
    }

    /** Evaluates against answers already cleaned by the validator; an unanswered field matches nothing. */
    static boolean holds(JsonObject condition, Map<String, JsonValue> answers) {
        if (condition.containsKey("all")) return condition.getJsonArray("all").stream().allMatch(c -> holds(c.asJsonObject(), answers));
        if (condition.containsKey("any")) return condition.getJsonArray("any").stream().anyMatch(c -> holds(c.asJsonObject(), answers));
        if (condition.containsKey("not")) return !holds(condition.getJsonObject("not"), answers);
        JsonValue answer = answers.get(condition.getString("field"));
        if (answer == null) return false;
        if (condition.containsKey("equals")) return FormJson.same(answer, condition.get("equals"));
        if (condition.containsKey("in")) return condition.getJsonArray("in").stream().anyMatch(v -> FormJson.same(answer, v));
        JsonValue wanted = condition.get("contains");
        return answer instanceof JsonArray chosen && chosen.stream().anyMatch(v -> FormJson.same(v, wanted));
    }

    private static boolean matches(String type, JsonObject field, JsonValue value) {
        return switch (type) {
            case "number" -> value instanceof JsonNumber;
            case "boolean" -> value == JsonValue.TRUE || value == JsonValue.FALSE;
            case "choice" -> value instanceof JsonString && options(field).contains(((JsonString) value).getString());
            default -> value instanceof JsonString;
        };
    }
    static Set<String> options(JsonObject field) {
        Set<String> values = new LinkedHashSet<>();
        if (field.get("options") instanceof JsonArray options)
            for (JsonValue option : options)
                if (option instanceof JsonObject o && FormJson.string(o, "value") != null) values.add(o.getString("value"));
        return values;
    }
    private static JsonObject fail(List<FormProblem> problems, String where, String message) {
        problems.add(new FormProblem(where, message));
        return null;
    }
}
