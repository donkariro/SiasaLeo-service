package com.arriyiaconsulting.siasaleo.service.security.authorization.boundary;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RolesAllowedFeature leaves an unannotated method open, which is right for
 * the public reference reads but means a new write endpoint is anonymous
 * until someone remembers to protect it. This test makes forgetting a build
 * failure: every POST, PUT, PATCH and DELETE must state its access with
 * @RolesAllowed, @PermitAll or @DenyAll, on the method or its class.
 * Deliberately public writes (login, registration) say so with @PermitAll.
 */
class WriteEndpointProtectionTest {

    private static final String BASE_PACKAGE = "com.arriyiaconsulting.siasaleo.service";

    @Test
    void everyWriteEndpointDeclaresItsAccess() throws Exception {
        List<Class<?>> resources = resourceClasses();
        assertFalse(resources.isEmpty(), "No @Path classes found; the scan is broken");

        List<String> unprotected = new ArrayList<>();
        for (Class<?> resource : resources) {
            for (Method method : resource.getDeclaredMethods()) {
                if (isWrite(method) && !declaresAccess(method) && !declaresAccess(resource)) {
                    unprotected.add(resource.getSimpleName() + "." + method.getName());
                }
            }
        }
        assertTrue(unprotected.isEmpty(),
                "Write endpoints without @RolesAllowed/@PermitAll/@DenyAll: " + unprotected);
    }

    private static boolean isWrite(Method method) {
        return method.isAnnotationPresent(POST.class) || method.isAnnotationPresent(PUT.class)
                || method.isAnnotationPresent(PATCH.class) || method.isAnnotationPresent(DELETE.class);
    }

    private static boolean declaresAccess(AnnotatedElement element) {
        return element.isAnnotationPresent(RolesAllowed.class)
                || element.isAnnotationPresent(PermitAll.class)
                || element.isAnnotationPresent(DenyAll.class);
    }

    // Walks the main classes directory rather than the test one: resources
    // live in main, and test-only classes must not satisfy or fail the check.
    private static List<Class<?>> resourceClasses()
            throws IOException, URISyntaxException, ClassNotFoundException {
        URL location = RolesAllowedFeature.class.getProtectionDomain().getCodeSource().getLocation();
        java.nio.file.Path classesRoot = Paths.get(location.toURI());
        java.nio.file.Path baseDir = classesRoot.resolve(BASE_PACKAGE.replace('.', '/'));

        List<Class<?>> resources = new ArrayList<>();
        try (Stream<java.nio.file.Path> files = Files.walk(baseDir)) {
            for (java.nio.file.Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                String name = classesRoot.relativize(file).toString()
                        .replace('\\', '/').replace('/', '.').replaceAll("\\.class$", "");
                Class<?> type = Class.forName(name, false, RolesAllowedFeature.class.getClassLoader());
                if (type.isAnnotationPresent(Path.class)) {
                    resources.add(type);
                }
            }
        }
        return resources;
    }
}
