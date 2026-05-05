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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.GET;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class GoogleAuthServiceImplTest {

    @Mock
    private GoogleOAuthProperties properties;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private MockRestServiceServer mockServer;
    private GoogleAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
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
    void authenticateWithAuthorizationCodeThrowsWhenBlank() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.authenticateWithAuthorizationCode(" "));
        assertNotNull(ex);
        mockServer.verify();
    }

    @Test
    void authenticateWithAuthorizationCodeThrowsWhenNull() {
        assertThrows(BusinessException.class, () -> service.authenticateWithAuthorizationCode(null));
    }

    @Test
    void exchangeCodeThrowsWhenClientSecretMissing() {
        mockServer.reset();
        when(properties.getClientSecret()).thenReturn(" ");

        assertThrows(BusinessException.class, () -> service.authenticateWithAuthorizationCode("code"));
    }

    @Test
    void exchangeCodeForAccessTokenThrowsWhenClientIdMissing() {
        when(properties.getClientId()).thenReturn("");
        mockServer.reset();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.authenticateWithAuthorizationCode("code"));
        assertNotNull(ex);
    }

    @Test
    void authenticateWithAuthorizationCodeCreatesUserWhenNew() {
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
    void authenticateWithAuthorizationCodeReusesExistingUser() {
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
    void fetchGoogleProfileThrowsWhenEmailMissing() {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"atok\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"name\":\"No Email\"}", MediaType.APPLICATION_JSON));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class, () -> service.authenticateWithAuthorizationCode("c"));
        assertNotNull(ex);
    }

    @Test
    void tokenExchangeFailsOnNon2xx() {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(ExternalServiceException.class, () -> service.authenticateWithAuthorizationCode("code"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tokenExchangeInvalidAccessTokenResponses")
    void tokenExchangeFailsWhenAccessTokenResponseInvalid(String scenario, String tokenResponseBody) {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(withSuccess(tokenResponseBody, MediaType.APPLICATION_JSON));

        assertThrows(ExternalServiceException.class, () -> service.authenticateWithAuthorizationCode("code"));
    }

    private static Stream<Arguments> tokenExchangeInvalidAccessTokenResponses() {
        return Stream.of(
                Arguments.of("missing access_token", "{}"),
                Arguments.of("blank access_token", "{\"access_token\":\"  \"}"),
                Arguments.of("invalid json", "not-json-{")
        );
    }

    @Test
    void tokenExchangeFailsOnRestClientException() {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(request -> {
                    throw HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "err", null, null, null);
                });

        assertThrows(ExternalServiceException.class, () -> service.authenticateWithAuthorizationCode("code"));
    }

    @Test
    void userinfoFailsOnNon2xx() {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"atok\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(ExternalServiceException.class, () -> service.authenticateWithAuthorizationCode("code"));
    }

    @Test
    void userinfoFailsOnInvalidJson() {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"atok\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{broken", MediaType.APPLICATION_JSON));

        assertThrows(ExternalServiceException.class, () -> service.authenticateWithAuthorizationCode("code"));
    }

    @Test
    void newUserUsesSuffixWhenUsernameTaken() {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"atok\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"email\":\"dup@example.com\",\"name\":\"Dup User\"}", MediaType.APPLICATION_JSON));

        when(userRepository.findByEmail("dup@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("dup")).thenReturn(true);
        when(userRepository.existsByUsername("dup1")).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        UserDto dto = service.authenticateWithAuthorizationCode("auth-code");

        assertEquals(99L, dto.getId());
        assertEquals("dup1", dto.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void newUserSanitizesUsernameFromEmailLocalPart() {
        mockServer.expect(requestTo(startsWith("https://oauth2.googleapis.com/token")))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"atok\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"email\":\"a+b@c.com\",\"name\":null}", MediaType.APPLICATION_JSON));

        when(userRepository.findByEmail("a+b@c.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("a_b")).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(3L);
            return u;
        });

        UserDto dto = service.authenticateWithAuthorizationCode("auth-code");

        assertEquals("a_b", dto.getUsername());
        assertEquals("Google", dto.getFirstName());
        assertEquals("User", dto.getLastName());
    }
}
