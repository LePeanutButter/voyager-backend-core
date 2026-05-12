package com.tourism.platform.integration.amadeus;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for the Amadeus Self-Service REST API (flights, hotels, activities catalogs).
 *
 * <p>By default the catalog runs in {@linkplain #isMockMode() mock mode} (Amadeus-shaped JSON, no API bill).
 * For live calls set {@code mock-mode=false} and provide OAuth2 credentials.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.integrations.amadeus")
public class AmadeusProperties {

    /**
     * When false, catalog endpoints return HTTP 400 (feature off).
     */
    private boolean enabled = true;

    /**
     * When true (default), responses are generated locally with the same JSON shapes as Amadeus; no API key required.
     */
    private boolean mockMode = true;

    /**
     * Base URL without trailing slash, e.g. {@code https://test.api.amadeus.com}.
     */
    private String apiHost = "https://test.api.amadeus.com";

    /** OAuth2 client id (consumer key). */
    private String clientId = "";

    /** OAuth2 client secret (consumer secret). */
    private String clientSecret = "";

    /**
     * Relative path for client-credentials token endpoint (configure via {@code app.integrations.amadeus.token-path},
     * e.g. in {@code application.yml} or env {@code AMADEUS_TOKEN_PATH}).
     */
    private String tokenPath;

    /**
     * Connect timeout applied to outbound Amadeus calls (milliseconds).
     */
    private int connectTimeoutMillis = 5_000;

    /**
     * Read timeout applied to outbound Amadeus calls (milliseconds).
     */
    private int readTimeoutMillis = 20_000;
}
