package com.tourism.platform.integration.amadeus;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Beans for outbound Amadeus HTTP calls ({@link RestClient} with timeouts).
 */
@Configuration
public class AmadeusIntegrationConfig {

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    RestClient amadeusRestClient(AmadeusProperties properties) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(properties.getConnectTimeoutMillis());
        rf.setReadTimeout(properties.getReadTimeoutMillis());

        String baseUrl = normalizeBaseUrl(properties.getApiHost());

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(rf)
                .build();
    }

    static String normalizeBaseUrl(String apiHost) {
        if (apiHost == null || apiHost.isBlank()) {
            return "https://test.api.amadeus.com";
        }
        String t = apiHost.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
