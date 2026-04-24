package com.tourism.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "microsoft.oauth2")
public class MicrosoftOAuthProperties {
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scopes;
}

