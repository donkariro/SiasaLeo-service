package com.arriyiaconsulting.siasaleo.service.security.authorization.boundary;

import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * Enforces @RolesAllowed, @PermitAll and @DenyAll on resource methods.
 *
 * Jakarta REST defines these annotations but does not itself apply them; the
 * implementations that do ship a feature for it do so under their own package
 * names. This is that feature, written against the spec so it does not tie the
 * application to Payara.
 *
 * A method with no annotation, on a class with none, is left open. That keeps
 * the electoral reference endpoints — the bulk of this API, and public data —
 * readable without a token, and means protection is something a resource opts
 * into. It also means a new endpoint is public until someone says otherwise,
 * so anything touching a user's own data must carry @RolesAllowed explicitly.
 */
@Provider
public class RolesAllowedFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Method method = resourceInfo.getResourceMethod();
        Class<?> resource = resourceInfo.getResourceClass();

        // Method-level annotations replace the class's rather than add to
        // them, so the first match wins and the class is consulted only when
        // the method is silent.
        if (method.isAnnotationPresent(DenyAll.class)) {
            context.register(RolesAllowedFilter.denyAll());
        } else if (method.isAnnotationPresent(RolesAllowed.class)) {
            context.register(RolesAllowedFilter.requiring(
                    method.getAnnotation(RolesAllowed.class)));
        } else if (method.isAnnotationPresent(PermitAll.class)) {
            // Explicitly open, even if the class restricts.
        } else if (resource.isAnnotationPresent(DenyAll.class)) {
            context.register(RolesAllowedFilter.denyAll());
        } else if (resource.isAnnotationPresent(RolesAllowed.class)) {
            context.register(RolesAllowedFilter.requiring(
                    resource.getAnnotation(RolesAllowed.class)));
        }
    }

    /**
     * 401 when nobody is authenticated and 403 when someone is but holds none
     * of the required roles: the first is fixable by logging in, the second is
     * not, and a client that cannot tell them apart retries forever.
     *
     * The policy is decided in {@link #decide} and only then encoded as a
     * response, so what the rule does can be tested without a JAX-RS runtime
     * standing behind it.
     */
    @Priority(Priorities.AUTHORIZATION)
    static final class RolesAllowedFilter implements ContainerRequestFilter {

        enum Decision {
            /** The caller holds a required role. */
            ALLOW,
            /** Nobody is authenticated; say so and how to fix it. */
            CHALLENGE,
            /** Authenticated, but not with a role that opens this. */
            FORBID
        }

        private final Set<String> allowed;

        private RolesAllowedFilter(Set<String> allowed) {
            this.allowed = allowed;
        }

        static RolesAllowedFilter requiring(RolesAllowed rolesAllowed) {
            return new RolesAllowedFilter(Set.of(rolesAllowed.value()));
        }

        /** No role satisfies an empty set, which is what @DenyAll means. */
        static RolesAllowedFilter denyAll() {
            return new RolesAllowedFilter(Set.of());
        }

        Decision decide(SecurityContext security) {
            if (security == null || security.getUserPrincipal() == null) {
                return Decision.CHALLENGE;
            }
            return allowed.stream().anyMatch(security::isUserInRole)
                    ? Decision.ALLOW : Decision.FORBID;
        }

        @Override
        public void filter(ContainerRequestContext request) {
            switch (decide(request.getSecurityContext())) {
                case ALLOW -> {
                    // Carry on to the resource method.
                }
                case CHALLENGE -> request.abortWith(
                        Response.status(Response.Status.UNAUTHORIZED)
                                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                                .type(MediaType.APPLICATION_JSON)
                                .entity(Map.of("error", "Authentication required"))
                                .build());
                case FORBID -> request.abortWith(
                        Response.status(Response.Status.FORBIDDEN)
                                .type(MediaType.APPLICATION_JSON)
                                .entity(Map.of("error",
                                        "You do not have access to this resource"))
                                .build());
            }
        }
    }
}
