package com.tourism.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI metadata and optional JWT bearer scheme (use Authorize in Swagger UI).
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_JWT = "bearer-jwt";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmarTrip Tourism Platform API")
                        .description(
                                "Core HTTP API (context path /api/v1). JWT: POST /users/login. Google OAuth: /auth/google. "
                                        + "Travel catalog (flights/hotels/activities): GET /catalog/* — default "
                                        + "AMADEUS_MOCK_MODE=true returns Amadeus-shaped JSON without API keys; set "
                                        + "AMADEUS_MOCK_MODE=false and credentials for live Amadeus (JWT required). "
                                        + "Real-time chat: WebSocket + STOMP at /ws-chat (see README), not OpenAPI REST.")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT returned in the login response (use raw token; Swagger adds Bearer).")));
    }
}
