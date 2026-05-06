package com.tourism.platform.owasptest;

import com.tourism.platform.controller.TravelPlanController;
import com.tourism.platform.controller.UserController;
import com.tourism.platform.dto.TravelPlanDto;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.dto.UserUpdateDto;
import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.model.TravelType;
import com.tourism.platform.model.User;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.SocialService;
import com.tourism.platform.service.TravelPlanActivityService;
import com.tourism.platform.service.TravelPlanService;
import com.tourism.platform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises {@code @PreAuthorize} for travel plans by user and profile updates (IDOR/BOLA mitigations).
 * Configuration lives in {@code com.tourism.platform.owasptest} so classpath scanning does not pick
 * {@link JwtTokenProvider} from {@code com.tourism.platform.security}.
 */
class PreAuthorizeResourceOwnershipTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Configuration
    static class TestBeans {

        @Bean
        TravelPlanActivityService travelPlanActivityService() {
            return Mockito.mock(TravelPlanActivityService.class);
        }

        @Bean
        SocialService socialService() {
            return Mockito.mock(SocialService.class);
        }

        @Bean
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        TravelPlanService travelPlanService() {
            return Mockito.mock(TravelPlanService.class);
        }

        @Bean
        UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        TravelPlanController travelPlanController(TravelPlanActivityService a, SocialService s, UserRepository u, TravelPlanService t) {
            return new TravelPlanController(a, s, u, t);
        }

        @Bean
        UserController userController(UserService users) {
            return new UserController(users, Mockito.mock(com.tourism.platform.security.JwtTokenProvider.class));
        }
    }

    private static AnnotationConfigApplicationContext context;
    private static TravelPlanController travelPlanController;
    private static UserController userController;
    private static TravelPlanService travelPlanService;
    private static UserService userService;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(MethodSecurityConfig.class, TestBeans.class);
        travelPlanController = context.getBean(TravelPlanController.class);
        userController = context.getBean(UserController.class);
        travelPlanService = context.getBean(TravelPlanService.class);
        userService = context.getBean(UserService.class);
    }

    @AfterAll
    static void stopContext() {
        if (context != null) {
            context.close();
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reset(travelPlanService, userService);
    }

    private static User domainUser(long id, String username, UserRole role) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@x.test")
                .password("pw")
                .firstName("F")
                .lastName("L")
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private static void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private static HttpServletRequest request(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRequestURI(uri);
        return r;
    }

    @Test
    void getTravelPlansByUser_deniesWhenNotOwnerNorAdmin() {
        authenticate(domainUser(1L, "u1", UserRole.TRAVELER));
        assertThatThrownBy(() -> travelPlanController.getTravelPlansByUser(99L, 0, 20, request("/travel-plans/user/99")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getTravelPlansByUser_allowsOwner() {
        TravelPlanDto plan = TravelPlanDto.builder()
                .id(1L)
                .title("T")
                .description("D")
                .status(TravelPlanStatus.ACTIVE)
                .travelType(TravelType.LEISURE)
                .destinationLocation("X")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .numberOfTravelers(1)
                .isPublic(false)
                .build();
        when(travelPlanService.getTravelPlanDtosByUser(2L)).thenReturn(List.of(plan));

        authenticate(domainUser(2L, "owner", UserRole.TRAVELER));
        assertDoesNotThrow(() -> travelPlanController.getTravelPlansByUser(2L, 0, 20, request("/travel-plans/user/2")));
        verify(travelPlanService).getTravelPlanDtosByUser(2L);
    }

    @Test
    void getTravelPlansByUser_allowsAdminForOtherUser() {
        when(travelPlanService.getTravelPlanDtosByUser(5L)).thenReturn(List.of());

        authenticate(domainUser(1L, "adm", UserRole.ADMIN));
        assertDoesNotThrow(() -> travelPlanController.getTravelPlansByUser(5L, 0, 20, request("/travel-plans/user/5")));
        verify(travelPlanService).getTravelPlanDtosByUser(5L);
    }

    @Test
    void updateUser_deniesWhenNotSelfNorAdmin() {
        authenticate(domainUser(3L, "u3", UserRole.TRAVELER));
        UserUpdateDto dto = new UserUpdateDto();
        dto.setFirstName("X");
        assertThatThrownBy(() -> userController.updateUser(8L, dto, request("/users/8")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateUser_allowsSelf() {
        UserDto out = new UserDto();
        out.setId(4L);
        out.setUsername("self");
        when(userService.updateUser(eq(4L), Mockito.any(UserUpdateDto.class))).thenReturn(Optional.of(out));

        authenticate(domainUser(4L, "self", UserRole.TRAVELER));
        UserUpdateDto dto = new UserUpdateDto();
        dto.setBio("bio");
        assertDoesNotThrow(() -> userController.updateUser(4L, dto, request("/users/4")));
        verify(userService).updateUser(eq(4L), Mockito.any(UserUpdateDto.class));
    }

    @Test
    void updateUser_allowsAdminForAnotherUser() {
        UserDto out = new UserDto();
        out.setId(10L);
        when(userService.updateUser(eq(10L), Mockito.any(UserUpdateDto.class))).thenReturn(Optional.of(out));

        authenticate(domainUser(1L, "adm", UserRole.ADMIN));
        UserUpdateDto dto = new UserUpdateDto();
        dto.setFirstName("A");
        assertDoesNotThrow(() -> userController.updateUser(10L, dto, request("/users/10")));
        verify(userService).updateUser(eq(10L), Mockito.any(UserUpdateDto.class));
    }
}
