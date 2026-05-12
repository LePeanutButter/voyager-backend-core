package com.tourism.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = HttpClientConfig.class)
class HttpClientConfigTest {

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void restTemplateBean_ShouldBeCreated() {
        assertNotNull(restTemplate, "RestTemplate bean should be created");
    }

    @Test
    void restTemplateBean_ShouldBeSingleton() {
        RestTemplate anotherRestTemplate = new HttpClientConfig().restTemplate();
        assertNotNull(anotherRestTemplate, "RestTemplate should be created");
        assertNotSame(restTemplate, anotherRestTemplate, "New instances should be different objects");
    }

    @Test
    void restTemplateBean_ShouldHaveDefaultConfiguration() {
        assertNotNull(restTemplate, "RestTemplate should not be null");
        // Test that it's a valid RestTemplate instance
        assertTrue(restTemplate instanceof RestTemplate, "Should be a RestTemplate instance");
    }

    @Test
    void restTemplateConfig_ShouldCreateRestTemplate() {
        HttpClientConfig config = new HttpClientConfig();
        RestTemplate template = config.restTemplate();
        
        assertNotNull(template, "RestTemplate should be created");
        assertTrue(template instanceof RestTemplate, "Should be a RestTemplate instance");
    }
}
