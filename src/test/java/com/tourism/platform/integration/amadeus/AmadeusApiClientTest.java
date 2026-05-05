package com.tourism.platform.integration.amadeus;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.tourism.platform.exception.ExternalServiceException;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AmadeusApiClientTest {

    private static final String BASE = "https://test.api.amadeus.com";

    @Mock
    private AmadeusTokenProvider tokenProvider;

    private MockRestServiceServer mockServer;
    private RestClient restClient;
    private AmadeusApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        client = new AmadeusApiClient(tokenProvider, restClient, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        mockServer.verify();
    }

    @Test
    void getJson_success() {
        when(tokenProvider.getAccessToken()).thenReturn("tok");
        mockServer
                .expect(requestTo(containsString("/v2/shopping/flight-offers")))
                .andRespond(withSuccess("{\"meta\":{\"count\":1}}", MediaType.APPLICATION_JSON));

        ObjectNode node = (ObjectNode) client.getJson(URI.create("/v2/shopping/flight-offers?originLocationCode=MAD"));

        assertEquals(1, node.path("meta").path("count").asInt());
    }

    @Test
    void getJson_on401_invalidatesAndRetriesOnce_thenSuccess() {
        when(tokenProvider.getAccessToken()).thenReturn("t1", "t2");
        mockServer
                .expect(requestTo(containsString("/v2/shopping/flight-offers")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        mockServer
                .expect(requestTo(containsString("/v2/shopping/flight-offers")))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        ObjectNode node = (ObjectNode) client.getJson(URI.create("/v2/shopping/flight-offers"));

        assertEquals(true, node.path("ok").asBoolean());
        verify(tokenProvider).invalidate();
    }

    @Test
    void getJson_on401Twice_throwsWithoutSecondInvalidate() {
        when(tokenProvider.getAccessToken()).thenReturn("same");
        mockServer
                .expect(requestTo(containsString("/v2/shopping/flight-offers")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        mockServer
                .expect(requestTo(containsString("/v2/shopping/flight-offers")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        final URI flightOffers = URI.create("/v2/shopping/flight-offers");
        assertThrows(ExternalServiceException.class, () -> client.getJson(flightOffers));

        verify(tokenProvider, times(1)).invalidate();
    }

    @Test
    void getJson_clientErrorNon401_throws() {
        when(tokenProvider.getAccessToken()).thenReturn("tok");
        mockServer
                .expect(requestTo(containsString("/v1/foo")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("bad"));

        final URI foo = URI.create("/v1/foo");
        assertThrows(ExternalServiceException.class, () -> client.getJson(foo));
    }

    @Test
    void getJson_serverError_throws() {
        when(tokenProvider.getAccessToken()).thenReturn("tok");
        mockServer
                .expect(requestTo(containsString("/v1/foo")))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        final URI foo = URI.create("/v1/foo");
        assertThrows(ExternalServiceException.class, () -> client.getJson(foo));
    }

    @Test
    void getJson_invalidJson_throws() {
        when(tokenProvider.getAccessToken()).thenReturn("tok");
        mockServer
                .expect(requestTo(containsString("/v1/foo")))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        final URI foo = URI.create("/v1/foo");
        assertThrows(ExternalServiceException.class, () -> client.getJson(foo));
    }
}
