package com.tourism.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Bean
    /**
     * Create and configure a `RestTemplate` bean for performing HTTP requests.
     *
     * @return a new RestTemplate instance suitable for synchronous HTTP calls
     */
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
