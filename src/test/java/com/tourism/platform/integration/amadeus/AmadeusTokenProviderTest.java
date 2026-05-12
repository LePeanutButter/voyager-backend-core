package com.tourism.platform.integration.amadeus;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.tourism.platform.exception.ExternalServiceException;

class AmadeusTokenProviderTest {

    private static final String BASE = "https://test.api.amadeus.com";
    private static final Instant T0 = Instant.parse("2026-06-01T12:00:00Z");

    private MockRestServiceServer mockServer;
    private RestClient restClient;
    private AmadeusProperties properties;
    private Clock clock;
    private AmadeusTokenProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        properties = new AmadeusProperties();
        properties.setClientId("cid");
        properties.setClientSecret("secret");
        properties.setTokenPath("/v1/security/oauth2/token");

        clock = Clock.fixed(T0, ZoneOffset.UTC);
        provider = new AmadeusTokenProvider(properties, restClient, new ObjectMapper(), clock);
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void getAccessToken_postsOnceThenUsesCache() {
        mockServer
                .expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(
                        withSuccess("{\"access_token\":\"abc\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        assertEquals("abc", provider.getAccessToken());
        assertEquals("abc", provider.getAccessToken());
    }

    @Test
    void getAccessToken_omittedExpiresIn_defaultsTo1800() {
        mockServer
                .expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(withSuccess("{\"access_token\":\"only\"}", MediaType.APPLICATION_JSON));

        assertEquals("only", provider.getAccessToken());
    }

    @Test
    void getAccessToken_smallExpiresIn_stillUsesAtLeast60SecondsForExpiry() {
        mockServer
                .expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(
                        withSuccess("{\"access_token\":\"short\",\"expires_in\":30}", MediaType.APPLICATION_JSON));

        assertEquals("short", provider.getAccessToken());
    }

    @Test
    void getAccessToken_httpError_throws() {
        mockServer
                .expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(ExternalServiceException.class, () -> provider.getAccessToken());
    }

    @Test
    void getAccessToken_missingAccessToken_throws() {
        mockServer
                .expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(withSuccess("{\"expires_in\":100}", MediaType.APPLICATION_JSON));

        assertThrows(ExternalServiceException.class, () -> provider.getAccessToken());
    }

    @Test
    void getAccessToken_malformedJson_throws() {
        mockServer
                .expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThrows(ExternalServiceException.class, () -> provider.getAccessToken());
    }

    @Test
    void getAccessToken_http500FromTokenEndpoint_throws() {
        mockServer
                .expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ExternalServiceException.class, () -> provider.getAccessToken());
    }

    @Test
    void getAccessToken_whenClockPassesSkewWindow_refreshesToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        AmadeusProperties props = new AmadeusProperties();
        props.setClientId("cid");
        props.setClientSecret("secret");
        props.setTokenPath("/v1/security/oauth2/token");

        MutableClock mutableClock = new MutableClock(T0, ZoneOffset.UTC);
        AmadeusTokenProvider p = new AmadeusTokenProvider(props, client, new ObjectMapper(), mutableClock);

        server.expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(
                        withSuccess("{\"access_token\":\"first\",\"expires_in\":120}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(
                        withSuccess("{\"access_token\":\"second\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        assertEquals("first", p.getAccessToken());
        mutableClock.setInstant(T0.plusSeconds(90));
        assertEquals("second", p.getAccessToken());

        server.verify();
    }

    @Test
    void invalidate_clearsCache_soNextCallPostsAgain() {
        mockServer
                .expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(
                        withSuccess("{\"access_token\":\"one\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(containsString("/v1/security/oauth2/token")))
                .andRespond(
                        withSuccess("{\"access_token\":\"two\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        assertEquals("one", provider.getAccessToken());
        provider.invalidate();
        assertEquals("two", provider.getAccessToken());
    }
}
