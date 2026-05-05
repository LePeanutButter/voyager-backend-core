package com.tourism.platform;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class TourismPlatformApplicationTest {

    @Test
    void main_delegatesToSpringApplication() {
        try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
            ConfigurableApplicationContext ctx = Mockito.mock(ConfigurableApplicationContext.class);
            spring.when(() -> SpringApplication.run(eq(TourismPlatformApplication.class), any(String[].class)))
                    .thenReturn(ctx);
            TourismPlatformApplication.main(new String[]{"--help"});
            spring.verify(() -> SpringApplication.run(eq(TourismPlatformApplication.class), any(String[].class)));
        }
    }
}
