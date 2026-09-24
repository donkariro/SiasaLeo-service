package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.mapping;
import java.math.BigDecimal;
public final class SqlValues {
    private SqlValues() { }
    public static Long number(Object value) { return value == null ? null : ((Number)value).longValue(); }
    public static String text(Object value) { return value == null ? null : value.toString(); }
    public static BigDecimal decimal(Object value) { return value == null ? null : new BigDecimal(value.toString()); }
}
