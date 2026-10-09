package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.boundary;

import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;

/** Asset visibility can change; do not allow shared caches to retain media metadata. */
@Provider
public class MediaResponseFilter implements ContainerResponseFilter {
    @Override public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        String path = request.getUriInfo().getPath();
        if (path.equals("digital-assets") || path.startsWith("digital-assets/")
                || path.startsWith("party-media/") || path.startsWith("parties/")) {
            response.getHeaders().putSingle("Cache-Control", "private, no-store");
            response.getHeaders().add("Vary", "Authorization");
        }
    }
}
