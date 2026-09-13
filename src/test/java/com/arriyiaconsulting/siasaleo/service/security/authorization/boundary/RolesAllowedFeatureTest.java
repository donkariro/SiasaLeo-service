package com.arriyiaconsulting.siasaleo.service.security.authorization.boundary;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import com.arriyiaconsulting.siasaleo.service.security.authorization.boundary.RolesAllowedFeature.RolesAllowedFilter;
import com.arriyiaconsulting.siasaleo.service.security.authorization.boundary.RolesAllowedFeature.RolesAllowedFilter.Decision;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.SecurityContext;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the annotation precedence @RolesAllowed enforcement follows,
 * and for the CHALLENGE/FORBID split that becomes 401 and 403. They assert on
 * Decision rather than on a Response, so no JAX-RS runtime is needed; turning
 * a decision into a response is the filter's one untested line.
 */
@ExtendWith(MockitoExtension.class)
class RolesAllowedFeatureTest {

    @RolesAllowed({"USER", "ADMINISTRATOR"})
    static class GuardedResource {

        void inherited() {
        }

        @RolesAllowed("ADMINISTRATOR")
        void adminOnly() {
        }

        // A method that opts out of the class's restriction entirely.
        @PermitAll
        void open() {
        }

        @DenyAll
        void closed() {
        }
    }

    static class OpenResource {

        void anything() {
        }
    }

    @Test
    void anUnannotatedMethodOnAnUnannotatedClassIsLeftOpen() {
        FeatureContext context = configure(OpenResource.class, "anything");
        verify(context, never()).register(any(Object.class));
    }

    @Test
    void aMethodInheritsItsClassRestriction() {
        RolesAllowedFilter filter = filterFor(GuardedResource.class, "inherited");

        assertEquals(Decision.ALLOW, filter.decide(authenticatedWith(Set.of("USER"))));
        assertEquals(Decision.FORBID,
                filter.decide(authenticatedWith(Set.of("SOMETHING_ELSE"))));
    }

    // The method's annotation replaces the class's rather than adding to it,
    // so USER no longer satisfies an ADMINISTRATOR-only method.
    @Test
    void aMethodRestrictionReplacesTheClassRestriction() {
        RolesAllowedFilter filter = filterFor(GuardedResource.class, "adminOnly");

        assertEquals(Decision.ALLOW, filter.decide(authenticatedWith(Set.of("ADMINISTRATOR"))));
        assertEquals(Decision.FORBID, filter.decide(authenticatedWith(Set.of("USER"))));
    }

    @Test
    void permitAllOnAMethodOpensItDespiteTheClass() {
        FeatureContext context = configure(GuardedResource.class, "open");
        verify(context, never()).register(any(Object.class));
    }

    @Test
    void denyAllRefusesEvenAnAdministrator() {
        assertEquals(Decision.FORBID, filterFor(GuardedResource.class, "closed")
                .decide(authenticatedWith(Set.of("ADMINISTRATOR"))));
    }

    // CHALLENGE, not FORBID: the caller has not identified themselves at all,
    // and a 401 tells them that logging in would help.
    @Test
    void anAnonymousCallerIsChallengedRatherThanForbidden() {
        assertEquals(Decision.CHALLENGE, filterFor(GuardedResource.class, "inherited")
                .decide(mock(SecurityContext.class)));
    }

    @Test
    void aRequestWithNoSecurityContextAtAllIsChallenged() {
        assertEquals(Decision.CHALLENGE,
                filterFor(GuardedResource.class, "inherited").decide(null));
    }

    /** Runs the feature and hands back the single filter it registered. */
    private static RolesAllowedFilter filterFor(Class<?> resource, String methodName) {
        FeatureContext context = configure(resource, methodName);
        ArgumentCaptor<Object> registered = ArgumentCaptor.forClass(Object.class);
        verify(context).register(registered.capture());
        return assertInstanceOf(RolesAllowedFilter.class, registered.getValue());
    }

    private static FeatureContext configure(Class<?> resource, String methodName) {
        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        when(resourceInfo.getResourceMethod()).thenReturn(methodOf(resource, methodName));
        when(resourceInfo.getResourceClass()).thenAnswer(invocation -> resource);
        FeatureContext context = mock(FeatureContext.class);
        new RolesAllowedFeature().configure(resourceInfo, context);
        return context;
    }

    private static SecurityContext authenticatedWith(Set<String> roles) {
        SecurityContext security = mock(SecurityContext.class);
        when(security.getUserPrincipal()).thenReturn(mock(Principal.class));
        // @DenyAll short-circuits on an empty role set and never asks;
        // lenient() keeps strict stubs happy.
        lenient().when(security.isUserInRole(any())).thenAnswer(
                invocation -> roles.contains(invocation.getArgument(0)));
        return security;
    }

    private static Method methodOf(Class<?> resource, String name) {
        try {
            return resource.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }
}
