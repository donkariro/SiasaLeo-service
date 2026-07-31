package com.arriyiaconsulting.siasaleo.service.shared.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

/**
 * Activates JAX-RS; all resources are served under /api.
 */
@ApplicationPath("api")
@OpenAPIDefinition(
        info = @Info(
                title = "SiasaLeo API",
                version = "1.0",
                description = "Electoral data service: electoral geography, parties, elections."))
public class ApplicationConfig extends Application {
}
