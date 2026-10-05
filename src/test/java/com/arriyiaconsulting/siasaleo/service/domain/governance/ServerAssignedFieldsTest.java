package com.arriyiaconsulting.siasaleo.service.domain.governance;

import com.arriyiaconsulting.siasaleo.service.domain.governance.performance.control.ClaimService;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Who recorded, created, issued, reviewed or published a governance record is
 * decided by the server from the authenticated account and the publication
 * workflow. Keeping those properties off every request type means a caller
 * cannot even attempt to set them, so this checks the request shapes rather
 * than each service method.
 */
class ServerAssignedFieldsTest {

    private static final List<String> PACKAGES = List.of(
            "com.arriyiaconsulting.siasaleo.service.domain.governance",
            "com.arriyiaconsulting.siasaleo.service.domain.digitalasset");

    private static final Set<String> SERVER_ASSIGNED = Set.of(
            "recordedByAccountId", "createdByAccountId", "reviewedByAccountId",
            "issuerPersonId", "publicationStatus", "publishedAt", "sealedAt");

    @Test
    void noRequestCarriesServerAssignedFields() throws Exception {
        List<Class<?>> requests = requestTypes();
        assertFalse(requests.isEmpty(), "No request types found; the scan is broken");

        List<String> offending = new ArrayList<>();
        for (Class<?> request : requests) {
            for (String property : propertiesOf(request)) {
                if (SERVER_ASSIGNED.contains(property)) {
                    offending.add(request.getSimpleName() + "." + property);
                }
            }
        }
        assertEquals(List.of(), offending, "Requests must not accept server-assigned fields");
    }

    private static List<String> propertiesOf(Class<?> type) {
        if (type.isRecord()) {
            return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
        }
        List<String> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            Arrays.stream(c.getDeclaredFields()).map(Field::getName).forEach(fields::add);
        }
        return fields;
    }

    // Walks the main classes directory, as WriteEndpointProtectionTest does,
    // so a new request type is checked without being listed here.
    private static List<Class<?>> requestTypes() throws Exception {
        URL location = ClaimService.class.getProtectionDomain().getCodeSource().getLocation();
        java.nio.file.Path classesRoot = Paths.get(location.toURI());
        List<Class<?>> found = new ArrayList<>();
        for (String pkg : PACKAGES) {
            java.nio.file.Path dir = classesRoot.resolve(pkg.replace('.', '/'));
            try (Stream<java.nio.file.Path> files = Files.walk(dir)) {
                for (java.nio.file.Path file : files.filter(f -> f.toString().endsWith("Request.class")).toList()) {
                    String name = classesRoot.relativize(file).toString()
                            .replace('\\', '/').replace('/', '.').replaceAll("\\.class$", "");
                    found.add(Class.forName(name, false, ServerAssignedFieldsTest.class.getClassLoader()));
                }
            }
        }
        return found;
    }
}
