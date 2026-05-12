package com.tourism.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "google.oauth2")
public class GoogleOAuthProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scopes;
    private String frontendRedirectUri;
}

