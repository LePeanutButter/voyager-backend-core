package com.tourism.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS settings. Supports exact origins, Ant-style origin patterns, an optional
 * preset for AWS-hosted frontends (ALB, S3 website, API Gateway-style hosts),
 * and an explicit allow-all escape hatch (credentials disabled).
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Comma-separated exact origins (e.g. {@code https://app.example.com}).
     */
    private String allowedOrigins = "http://localhost:5173";

    /**
     * Comma-separated {@link org.springframework.web.cors.CorsConfiguration#setAllowedOriginPatterns}.
     */
    private String allowedOriginPatterns = "";

    /**
     * When true, sets allowed origin pattern {@code *} and forces credentials off (browser CORS rules).
     */
    private boolean allowAllOrigins = false;

    /**
     * Adds a conservative preset of patterns matching typical AWS hostnames (not arbitrary internet origins).
     */
    private boolean useAwsHostnamePatterns = false;

    /**
     * Whether to allow cookies / Authorization with CORS. Ignored when {@link #allowAllOrigins} is true.
     */
    private boolean allowCredentials = true;

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(String allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    public boolean isAllowAllOrigins() {
        return allowAllOrigins;
    }

    public void setAllowAllOrigins(boolean allowAllOrigins) {
        this.allowAllOrigins = allowAllOrigins;
    }

    public boolean isUseAwsHostnamePatterns() {
        return useAwsHostnamePatterns;
    }

    public void setUseAwsHostnamePatterns(boolean useAwsHostnamePatterns) {
        this.useAwsHostnamePatterns = useAwsHostnamePatterns;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }
}
