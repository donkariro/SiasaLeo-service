package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.importing;
import java.nio.charset.StandardCharsets;
import java.security.*;
import jakarta.validation.Validator;
import java.util.*;
public final class ImportSupport {
    private ImportSupport() { }
    public record ValidationReport(boolean valid, List<String> errors) { }
    /** Bean Validation failures as "path: message", sorted so a report reads the same every time. */
    public static List<String> violations(Validator validator, Object request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
    }
    public static <T> List<T> list(List<T> values) { return values==null?List.of():values; }
    public static boolean positive(Long n) { return n!=null && n>0; }
    public static boolean text(String s,int max) { return s!=null && !s.isBlank() && s.length()<=max; }
    public static void requireValid(ValidationReport report) {
        if(!report.valid()) throw new IllegalArgumentException(String.join("; ",report.errors()));
    }
    public static String fingerprint(Object r) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(r).getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String canonical(Object value) {
        if(value==null) return "N;";
        if(value instanceof List<?> list) return "L"+list.size()+":"+list.stream().map(ImportSupport::canonical).collect(java.util.stream.Collectors.joining());
        if(value.getClass().isRecord()) {
            StringBuilder out=new StringBuilder("R:");
            for(var component:value.getClass().getRecordComponents()) {
                try { out.append(canonical(component.getAccessor().invoke(value))); }
                catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
            }
            return out.toString();
        }
        String text=value.toString();
        return value.getClass().getSimpleName()+text.length()+":"+text;
    }
    public static int size(int size) { return Math.max(1,Math.min(size,500)); }
    public static int offset(int page,int size) {
        if(page<0 || (long)page*size(size)>Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid page");
        return page*size(size);
    }}
