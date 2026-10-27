package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import com.arriyiaconsulting.siasaleo.service.domain.forms.dto.FormProblem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Turns authored JSON into the resolved definition that renderers display and
 * FormValidator enforces (docs/forms.md#definition-format). A platform core
 * compiles on its own; a tenant extension compiles against the exact resolved
 * core version it names, within that core's extensionPolicy. Keys are
 * qualified on the way through ("core.phone", "ext.partyService"), so an
 * extension can never collide with or redefine a core field.
 */
@ApplicationScoped
public class FormCompiler {
    public static final int FORMAT = 1;
    static final String CORE = "core", EXTENSION = "ext";
    static final Set<String> TYPES = Set.of("text", "textarea", "number", "date", "boolean", "choice", "multichoice", "document");
    private static final Pattern KEY = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]{0,99}");
    private static final Pattern ASSET_TYPE = Pattern.compile("[A-Z][A-Z0-9_]{0,39}");
    private static final int MAX_TEXT = 20000;
    private static final Set<String> COMMON = Set.of("key", "type", "label", "help", "required", "visibleWhen", "requiredWhen", "origin");
    private static final Map<String, Set<String>> TYPE_ATTRIBUTES = Map.of(
            "text", Set.of("minLength", "maxLength"),
            "textarea", Set.of("minLength", "maxLength"),
            "number", Set.of("min", "max", "integer"),
            "date", Set.of("min", "max"),
            "boolean", Set.of(),
            "choice", Set.of("options"),
            "multichoice", Set.of("options", "minItems", "maxItems"),
            "document", Set.of("assetType", "multiple"));

    /** A section whose field keys are already qualified; conditions are qualified during assembly. */
    private record Section(String key, String title, String description, JsonValue visibleWhen, String origin, List<JsonObject> fields) {}

    public JsonObject core(JsonObject source) {
        List<FormProblem> problems = new ArrayList<>();
        attributes(source, Set.of("format", "title", "sections", "declarations", "extensionPolicy"), null, problems);
        format(source, problems);
        String title = text(source, "title", 255, true, null, problems);
        List<Section> sections = new ArrayList<>();
        for (JsonObject raw : objects(source.get("sections"), "sections", problems)) {
            Section section = authored(raw, CORE, problems);
            if (section != null) sections.add(section);
        }
        if (sections.isEmpty()) problems.add(new FormProblem("sections", "A core form needs at least one section"));
        List<JsonObject> declarations = declarations(source.get("declarations"), CORE, problems);
        JsonObject resolved = assemble(title, sections, declarations, problems);
        JsonObject policy = policy(source.get("extensionPolicy"), sections, problems);
        finish(problems);
        return Json.createObjectBuilder(resolved).add("extensionPolicy", policy).build();
    }

    public JsonObject extend(JsonObject core, JsonObject source) {
        List<FormProblem> problems = new ArrayList<>();
        attributes(source, Set.of("format", "help", "addFields", "addSections", "addDeclarations"), null, problems);
        format(source, problems);
        JsonObject policy = core.getJsonObject("extensionPolicy");
        List<Section> sections = new ArrayList<>();
        for (JsonValue raw : core.getJsonArray("sections")) sections.add(resolved(raw.asJsonObject()));

        if (source.get("help") != null) {
            if (!(source.get("help") instanceof JsonObject help)) problems.add(new FormProblem("help", "help maps core field keys to help text"));
            else help.forEach((key, value) -> {
                if (!strings(policy, "helpEditable").contains(key))
                    problems.add(new FormProblem(key, "The core does not allow help text changes on this field"));
                else if (!(value instanceof JsonString text) || text.getString().length() > 2000)
                    problems.add(new FormProblem(key, "Help text is a string of at most 2000 characters"));
                else replaceHelp(sections, CORE + "." + key, text.getString());
            });
        }
        for (JsonObject addition : objects(source.get("addFields"), "addFields", problems)) {
            attributes(addition, Set.of("section", "fields"), "addFields", problems);
            String target = FormJson.string(addition, "section");
            Section section = target == null ? null : sections.stream()
                    .filter(s -> s.key().equals(CORE + "." + target)).findFirst().orElse(null);
            if (section == null || !strings(policy, "addFieldsTo").contains(target)) {
                problems.add(new FormProblem(target, "The core does not allow additional fields in this section"));
                continue;
            }
            for (JsonObject field : objects(addition.get("fields"), target, problems)) {
                JsonObject qualified = qualify(field, EXTENSION, problems);
                if (qualified != null) section.fields().add(qualified);
            }
        }
        List<JsonObject> added = objects(source.get("addSections"), "addSections", problems);
        if (!added.isEmpty() && !FormJson.flag(policy, "addSections"))
            problems.add(new FormProblem("addSections", "The core does not allow additional sections"));
        else for (JsonObject raw : added) {
            Section section = authored(raw, EXTENSION, problems);
            if (section != null) sections.add(section);
        }
        List<JsonObject> declarations = new ArrayList<>();
        core.getJsonArray("declarations").forEach(d -> declarations.add(d.asJsonObject()));
        JsonValue extra = source.get("addDeclarations");
        if (extra != null && !FormJson.flag(policy, "addDeclarations"))
            problems.add(new FormProblem("addDeclarations", "The core does not allow additional declarations"));
        else declarations.addAll(declarations(extra, EXTENSION, problems));

        JsonObject resolved = assemble(core.getString("title"), sections, declarations, problems);
        finish(problems);
        return resolved;
    }

    /** One ordered pass: every condition may only look at fields that come before it. */
    private JsonObject assemble(String title, List<Section> sections, List<JsonObject> declarations, List<FormProblem> problems) {
        Map<String, JsonObject> earlier = new LinkedHashMap<>();
        Set<String> sectionKeys = new HashSet<>();
        JsonArrayBuilder out = Json.createArrayBuilder();
        for (Section section : sections) {
            if (!sectionKeys.add(section.key())) problems.add(new FormProblem(section.key(), "Duplicate section key"));
            JsonObjectBuilder built = Json.createObjectBuilder().add("key", section.key())
                    .add("title", section.title()).add("origin", section.origin());
            if (section.description() != null) built.add("description", section.description());
            if (section.visibleWhen() != null) {
                JsonObject condition = Conditions.compile(section.visibleWhen(), section.origin(), earlier, section.key(), problems);
                if (condition != null) built.add("visibleWhen", condition);
            }
            JsonArrayBuilder fields = Json.createArrayBuilder();
            for (JsonObject raw : section.fields()) {
                JsonObject field = field(raw, earlier, problems);
                if (field == null) continue;
                if (earlier.putIfAbsent(field.getString("key"), field) != null)
                    problems.add(new FormProblem(field.getString("key"), "Duplicate field key"));
                else fields.add(field);
            }
            out.add(built.add("fields", fields));
        }
        Set<String> declarationKeys = new HashSet<>();
        JsonArrayBuilder declared = Json.createArrayBuilder();
        for (JsonObject declaration : declarations) {
            if (!declarationKeys.add(declaration.getString("key")))
                problems.add(new FormProblem(declaration.getString("key"), "Duplicate declaration key"));
            declared.add(declaration);
        }
        return Json.createObjectBuilder().add("format", FORMAT).add("title", title == null ? "" : title)
                .add("sections", out).add("declarations", declared).build();
    }

    private JsonObject field(JsonObject raw, Map<String, JsonObject> earlier, List<FormProblem> problems) {
        String key = raw.getString("key");
        String origin = raw.getString("origin");
        String type = FormJson.string(raw, "type");
        if (type == null || !TYPES.contains(type)) {
            problems.add(new FormProblem(key, "Unsupported field type: " + type));
            return null;
        }
        Set<String> allowed = new HashSet<>(COMMON);
        allowed.addAll(TYPE_ATTRIBUTES.get(type));
        int before = problems.size();
        attributes(raw, allowed, key, problems);
        JsonObjectBuilder field = Json.createObjectBuilder().add("key", key).add("type", type).add("origin", origin)
                .add("label", Objects.requireNonNullElse(text(raw, "label", 500, true, key, problems), ""));
        String help = text(raw, "help", 2000, false, key, problems);
        if (help != null) field.add("help", help);
        boolean required = bool(raw, "required", key, problems);
        field.add("required", required);
        switch (type) {
            case "text", "textarea" -> {
                Integer min = integer(raw, "minLength", key, problems);
                Integer max = integer(raw, "maxLength", key, problems);
                int limit = max == null ? ("text".equals(type) ? 255 : 5000) : max;
                if (limit > MAX_TEXT || (min != null && min > limit))
                    problems.add(new FormProblem(key, "Lengths need 0 <= minLength <= maxLength <= " + MAX_TEXT));
                if (min != null) field.add("minLength", min);
                field.add("maxLength", limit);
            }
            case "number" -> {
                JsonValue min = raw.get("min"), max = raw.get("max");
                if ((min != null && !(min instanceof JsonNumber)) || (max != null && !(max instanceof JsonNumber))
                        || (min != null && max != null && FormJson.decimal(min).compareTo(FormJson.decimal(max)) > 0))
                    problems.add(new FormProblem(key, "min and max are numbers with min <= max"));
                if (min != null) field.add("min", min);
                if (max != null) field.add("max", max);
                field.add("integer", bool(raw, "integer", key, problems));
            }
            case "date" -> {
                LocalDate min = date(raw, "min", key, problems), max = date(raw, "max", key, problems);
                if (min != null && max != null && min.isAfter(max)) problems.add(new FormProblem(key, "min is after max"));
                if (min != null) field.add("min", min.toString());
                if (max != null) field.add("max", max.toString());
            }
            case "choice", "multichoice" -> {
                JsonArray options = options(raw, key, problems);
                field.add("options", options);
                if ("multichoice".equals(type)) {
                    Integer min = integer(raw, "minItems", key, problems), max = integer(raw, "maxItems", key, problems);
                    if ((max != null && max > options.size()) || (min != null && max != null && min > max)
                            || (min != null && min > options.size()))
                        problems.add(new FormProblem(key, "Item limits need 0 <= minItems <= maxItems <= number of options"));
                    if (min != null) field.add("minItems", min);
                    if (max != null) field.add("maxItems", max);
                }
            }
            case "document" -> {
                String assetType = FormJson.string(raw, "assetType");
                if (raw.containsKey("assetType") && (assetType == null || !ASSET_TYPE.matcher(assetType).matches()))
                    problems.add(new FormProblem(key, "assetType is an upper-case asset type code"));
                if (assetType != null) field.add("assetType", assetType);
                field.add("multiple", bool(raw, "multiple", key, problems));
            }
            default -> { }
        }
        if (raw.containsKey("visibleWhen")) {
            JsonObject condition = Conditions.compile(raw.get("visibleWhen"), origin, earlier, key, problems);
            if (condition != null) field.add("visibleWhen", condition);
        }
        if (raw.containsKey("requiredWhen")) {
            if (required) problems.add(new FormProblem(key, "Use required or requiredWhen, not both"));
            JsonObject condition = Conditions.compile(raw.get("requiredWhen"), origin, earlier, key, problems);
            if (condition != null) field.add("requiredWhen", condition);
        }
        return problems.size() == before ? field.build() : null;
    }

    private JsonArray options(JsonObject raw, String key, List<FormProblem> problems) {
        JsonArrayBuilder out = Json.createArrayBuilder();
        Set<String> values = new HashSet<>();
        List<JsonObject> options = objects(raw.get("options"), key, problems);
        if (options.isEmpty()) problems.add(new FormProblem(key, "A choice field needs options"));
        for (JsonObject option : options) {
            attributes(option, Set.of("value", "label"), key, problems);
            String value = text(option, "value", 100, true, key, problems);
            String label = text(option, "label", 500, true, key, problems);
            if (value == null || label == null) continue;
            if (!values.add(value)) problems.add(new FormProblem(key, "Duplicate option value: " + value));
            out.add(Json.createObjectBuilder().add("value", value).add("label", label));
        }
        return out.build();
    }

    private List<JsonObject> declarations(JsonValue raw, String origin, List<FormProblem> problems) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonObject declaration : objects(raw, "declarations", problems)) {
            attributes(declaration, Set.of("key", "text"), "declarations", problems);
            String key = FormJson.string(declaration, "key");
            String text = text(declaration, "text", 10000, true, key, problems);
            if (key == null || !KEY.matcher(key).matches()) {
                problems.add(new FormProblem("declarations", "Declaration keys are letters, digits and underscores"));
                continue;
            }
            if (text != null) out.add(Json.createObjectBuilder().add("key", origin + "." + key).add("text", text).add("origin", origin).build());
        }
        return out;
    }

    private JsonObject policy(JsonValue raw, List<Section> sections, List<FormProblem> problems) {
        JsonObject policy = raw == null ? JsonValue.EMPTY_JSON_OBJECT : raw instanceof JsonObject o ? o : null;
        if (policy == null) {
            problems.add(new FormProblem("extensionPolicy", "extensionPolicy is an object"));
            policy = JsonValue.EMPTY_JSON_OBJECT;
        }
        attributes(policy, Set.of("addSections", "addFieldsTo", "helpEditable", "addDeclarations"), "extensionPolicy", problems);
        Set<String> sectionKeys = new HashSet<>(), fieldKeys = new HashSet<>();
        for (Section section : sections) {
            sectionKeys.add(section.key().substring(CORE.length() + 1));
            section.fields().forEach(f -> fieldKeys.add(f.getString("key").substring(CORE.length() + 1)));
        }
        List<String> addFieldsTo = strings(policy, "addFieldsTo"), helpEditable = strings(policy, "helpEditable");
        addFieldsTo.stream().filter(k -> !sectionKeys.contains(k))
                .forEach(k -> problems.add(new FormProblem("extensionPolicy", "Unknown section in addFieldsTo: " + k)));
        helpEditable.stream().filter(k -> !fieldKeys.contains(k))
                .forEach(k -> problems.add(new FormProblem("extensionPolicy", "Unknown field in helpEditable: " + k)));
        return Json.createObjectBuilder()
                .add("addSections", bool(policy, "addSections", "extensionPolicy", problems))
                .add("addFieldsTo", Json.createArrayBuilder(addFieldsTo))
                .add("helpEditable", Json.createArrayBuilder(helpEditable))
                .add("addDeclarations", bool(policy, "addDeclarations", "extensionPolicy", problems)).build();
    }

    private Section authored(JsonObject raw, String origin, List<FormProblem> problems) {
        attributes(raw, Set.of("key", "title", "description", "visibleWhen", "fields"), "sections", problems);
        String key = FormJson.string(raw, "key");
        if (key == null || !KEY.matcher(key).matches()) {
            problems.add(new FormProblem("sections", "Section keys are letters, digits and underscores: " + key));
            return null;
        }
        String title = text(raw, "title", 255, true, key, problems);
        String description = text(raw, "description", 5000, false, key, problems);
        List<JsonObject> fields = new ArrayList<>();
        for (JsonObject field : objects(raw.get("fields"), key, problems)) {
            JsonObject qualified = qualify(field, origin, problems);
            if (qualified != null) fields.add(qualified);
        }
        return new Section(origin + "." + key, title == null ? "" : title, description, raw.get("visibleWhen"), origin, fields);
    }

    private static Section resolved(JsonObject section) {
        List<JsonObject> fields = new ArrayList<>();
        section.getJsonArray("fields").forEach(f -> fields.add(f.asJsonObject()));
        return new Section(section.getString("key"), section.getString("title"), FormJson.string(section, "description"),
                section.get("visibleWhen"), section.getString("origin"), fields);
    }

    private static JsonObject qualify(JsonObject field, String origin, List<FormProblem> problems) {
        String key = FormJson.string(field, "key");
        if (key == null || !KEY.matcher(key).matches()) {
            problems.add(new FormProblem(key, "Field keys are letters, digits and underscores, starting with a letter"));
            return null;
        }
        if (field.containsKey("origin")) {
            problems.add(new FormProblem(key, "origin is assigned by the compiler"));
            return null;
        }
        return Json.createObjectBuilder(field).add("key", origin + "." + key).add("origin", origin).build();
    }

    private static void replaceHelp(List<Section> sections, String key, String help) {
        for (Section section : sections)
            section.fields().replaceAll(f -> f.getString("key").equals(key) ? Json.createObjectBuilder(f).add("help", help).build() : f);
    }

    private static void format(JsonObject source, List<FormProblem> problems) {
        if (!(source.get("format") instanceof JsonNumber n) || !n.isIntegral() || n.intValue() != FORMAT)
            problems.add(new FormProblem("format", "Set format to " + FORMAT));
    }
    private static void attributes(JsonObject object, Set<String> allowed, String where, List<FormProblem> problems) {
        object.keySet().stream().filter(k -> !allowed.contains(k)).sorted()
                .forEach(k -> problems.add(new FormProblem(where, "Unknown attribute: " + k)));
    }
    private static List<JsonObject> objects(JsonValue raw, String where, List<FormProblem> problems) {
        if (raw == null) return List.of();
        if (!(raw instanceof JsonArray array) || !array.stream().allMatch(v -> v instanceof JsonObject)) {
            problems.add(new FormProblem(where, "Expected a list of objects"));
            return List.of();
        }
        return array.stream().map(JsonValue::asJsonObject).toList();
    }
    private static List<String> strings(JsonObject object, String name) {
        if (!(object.get(name) instanceof JsonArray array)) return List.of();
        return array.stream().filter(v -> v instanceof JsonString).map(v -> ((JsonString) v).getString()).toList();
    }
    private static String text(JsonObject object, String name, int max, boolean required, String where, List<FormProblem> problems) {
        JsonValue value = object.get(name);
        if (value == null && !required) return null;
        if (!(value instanceof JsonString text) || text.getString().isBlank() || text.getString().length() > max) {
            problems.add(new FormProblem(where, name + " is " + (required ? "required text" : "text") + " of at most " + max + " characters"));
            return null;
        }
        return text.getString();
    }
    private static boolean bool(JsonObject object, String name, String where, List<FormProblem> problems) {
        JsonValue value = object.get(name);
        if (value != null && value != JsonValue.TRUE && value != JsonValue.FALSE) problems.add(new FormProblem(where, name + " is true or false"));
        return value == JsonValue.TRUE;
    }
    private static Integer integer(JsonObject object, String name, String where, List<FormProblem> problems) {
        JsonValue value = object.get(name);
        if (value == null) return null;
        if (value instanceof JsonNumber n && n.isIntegral() && n.longValue() >= 0 && n.longValue() <= MAX_TEXT) return n.intValue();
        problems.add(new FormProblem(where, name + " is a whole number from 0 to " + MAX_TEXT));
        return null;
    }
    private static LocalDate date(JsonObject object, String name, String where, List<FormProblem> problems) {
        JsonValue value = object.get(name);
        if (value == null) return null;
        try { return LocalDate.parse(((JsonString) value).getString()); }
        catch (ClassCastException | DateTimeParseException e) {
            problems.add(new FormProblem(where, name + " is an ISO date (yyyy-mm-dd)"));
            return null;
        }
    }
    private static void finish(List<FormProblem> problems) {
        if (!problems.isEmpty()) throw new InvalidFormException("The form definition is invalid", problems);
    }
}
