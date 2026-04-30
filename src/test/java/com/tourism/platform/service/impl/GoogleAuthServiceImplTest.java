package com.tourism.platform.service.impl;

import com.tourism.platform.config.GoogleOAuthProperties;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.exception.ExternalServiceException;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.GET;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceImplTest {

    @Mock
    private GoogleOAuthProperties properties;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private GoogleAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        service = new GoogleAuthServiceImpl(properties, userRepository, passwordEncoder, jwtTokenProvider, restTemplate);

        lenient().when(properties.getClientId()).thenReturn("client-id");
        lenient().when(properties.getClientSecret()).thenReturn("secret");
        lenient().when(properties.getRedirectUri()).thenReturn("http://localhost/cb");
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        lenient().when(jwtTokenProvider.generateTokenFromUsernameAndUserId(anyString(), anyLong())).thenReturn("jwt");
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void authenticateWithAuthorizationCode_ThrowsWhenBlank() {
        assertThrows(BusinessException.class, () -> service.authenticateWithAuthorizationCode(" "));
        mockServer.verify();
    }

    @Test
    void exchangeCodeForAccessToken_ThrowsWhenClientIdMissing() {
        when(properties.getClientId()).thenReturn("");
        mockServer.reset();

        assertThrows(BusinessException.class, () -> service.authenticateWithAuthorizationCode("code"));
    }

    @Test
    void authenticateWithAuthorizationCode_CreatesUser_WhenNew() {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"atok\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"email\":\"new@example.com\",\"name\":\"New User\"}", MediaType.APPLICATION_JSON));

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("new")).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        UserDto dto = service.authenticateWithAuthorizationCode("auth-code");

        assertEquals("jwt", dto.getToken());
        assertEquals(42L, dto.getId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void authenticateWithAuthorizationCode_ReusesExistingUser() {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"atok\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"email\":\"exist@example.com\",\"name\":\"Old User\"}", MediaType.APPLICATION_JSON));

        User existing = new User();
        existing.setId(7L);
        existing.setUsername("exist");
        existing.setEmail("exist@example.com");
        when(userRepository.findByEmail("exist@example.com")).thenReturn(Optional.of(existing));

        UserDto dto = service.authenticateWithAuthorizationCode("auth-code");

        assertEquals(7L, dto.getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    void fetchGoogleProfile_ThrowsWhenEmailMissing() {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"atok\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"name\":\"No Email\"}", MediaType.APPLICATION_JSON));

        assertThrows(ExternalServiceException.class, () -> service.authenticateWithAuthorizationCode("c"));
    }
}
