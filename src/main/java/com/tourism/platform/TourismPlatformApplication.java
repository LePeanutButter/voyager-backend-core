package com.tourism.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for Tourism Intelligent Platform
 * 
 * This class serves as the entry point for the Spring Boot application.
 * It enables JPA auditing for automatic timestamp management.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
public class TourismPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourismPlatformApplication.class, args);
    }
}
