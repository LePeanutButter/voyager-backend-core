package com.tourism.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ensures springdoc can serialize OpenAPI without a 500 (regression guard for Swagger UI).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:openapitest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // Satisfy application.yml placeholders before beans bind (CI has no .env)
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "JWT_SECRET=test-jwt-secret-for-openapi-test-1234567890",
        "spring.security.jwt.secret=test-jwt-secret-for-openapi-test-1234567890",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
class OpenApiAvailabilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsJson_isOk() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk());
    }
}
