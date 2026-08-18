package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CORS support for browser clients. Preflight OPTIONS requests are answered
 * before resource matching (so they need no OPTIONS methods on resources);
 * the response filter tags every cross-origin response.
 *
 * Allowed origins come from the CORS_ALLOWED_ORIGINS environment variable
 * (comma-separated, e.g. "https://app.siasaleo.com,http://localhost:5173");
 * unset means "*", which is only acceptable for development.
 */
@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ALLOWED_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    private static final String ALLOWED_HEADERS = "Content-Type, Authorization";
    private static final String MAX_AGE_SECONDS = "86400";

    private final Set<String> allowedOrigins;
    private final boolean allowAll;

    public CorsFilter() {
        this(System.getenv("CORS_ALLOWED_ORIGINS"));
    }

    CorsFilter(String configuredOrigins) {
        this.allowedOrigins = configuredOrigins == null || configuredOrigins.isBlank()
                ? Set.of("*")
                : Arrays.stream(configuredOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        this.allowAll = allowedOrigins.contains("*");
    }

    @Override
    public void filter(ContainerRequestContext request) {
        if ("OPTIONS".equals(request.getMethod())
                && request.getHeaderString("Origin") != null
                && request.getHeaderString("Access-Control-Request-Method") != null) {
            // Headers are added by the response filter below.
            request.abortWith(Response.ok().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        String origin = request.getHeaderString("Origin");
        if (origin == null || !(allowAll || allowedOrigins.contains(origin))) {
            return;
        }
        var headers = response.getHeaders();
        headers.putSingle("Access-Control-Allow-Origin", allowAll ? "*" : origin);
        if (!allowAll) {
            // The response depends on the Origin header, so caches must key on it;
            // credentials are only allowed when origins are pinned.
            headers.add(HttpHeaders.VARY, "Origin");
            headers.putSingle("Access-Control-Allow-Credentials", "true");
        }
        headers.putSingle("Access-Control-Allow-Methods", ALLOWED_METHODS);
        headers.putSingle("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        headers.putSingle("Access-Control-Max-Age", MAX_AGE_SECONDS);
    }
}
