package com.arriyiaconsulting.siasaleo.service.domain.forms.control;

import jakarta.json.*;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.*;

/** JSON-P helpers shared by the compiler, validator and native persistence. */
public final class FormJson {
    private FormJson() { }

    public static JsonObject parse(String text) {
        if (text == null) return null;
        try (JsonReader reader = Json.createReader(new StringReader(text))) { return reader.readObject(); }
    }
    public static String text(JsonObject value) { return value == null ? null : value.toString(); }

    /** Native timestamptz columns arrive as Timestamp, OffsetDateTime or Instant depending on the driver path. */
    public static OffsetDateTime time(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime time) return time;
        if (value instanceof java.sql.Timestamp time) return time.toInstant().atOffset(ZoneOffset.UTC);
        if (value instanceof Instant time) return time.atOffset(ZoneOffset.UTC);
        return OffsetDateTime.parse(value.toString());
    }

    static String string(JsonObject object, String name) {
        JsonValue value = object.get(name);
        return value instanceof JsonString text ? text.getString() : null;
    }
    static boolean flag(JsonObject object, String name) { return object.get(name) == JsonValue.TRUE; }

    /** Numbers compare by value so 1, 1.0 and 1e0 are one answer. */
    static boolean same(JsonValue a, JsonValue b) {
        if (a instanceof JsonNumber x && b instanceof JsonNumber y) return x.bigDecimalValue().compareTo(y.bigDecimalValue()) == 0;
        return a != null && a.equals(b);
    }
    static boolean scalar(JsonValue value) {
        return value instanceof JsonString || value instanceof JsonNumber || value == JsonValue.TRUE || value == JsonValue.FALSE;
    }
    static BigDecimal decimal(JsonValue value) { return value instanceof JsonNumber number ? number.bigDecimalValue() : null; }
}
