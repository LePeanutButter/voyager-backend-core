package com.tourism.platform.integration.amadeus;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.tourism.platform.exception.ExternalServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Outbound GET calls to Amadeus (OAuth bearer). Used only when {@link AmadeusProperties#isMockMode()} is false.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AmadeusApiClient {

    private final AmadeusTokenProvider tokenProvider;
    private final RestClient amadeusRestClient;
    private final ObjectMapper objectMapper;

    JsonNode getJson(URI uri) {
        return exchangeGet(uri, true);
    }

    private JsonNode exchangeGet(URI uri, boolean mayRetryUnauthorized) {
        String bearer = tokenProvider.getAccessToken();
        try {
            String raw = fetchRaw(uri, bearer);
            return objectMapper.readTree(raw);
        } catch (HttpClientErrorException e) {
            if (mayRetryUnauthorized && e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                log.info("event=amadeus_retry_after_http401 path={}", uri.getPath());
                tokenProvider.invalidate();
                return exchangeGet(uri, false);
            }
            throw new ExternalServiceException("Amadeus HTTP " + e.getStatusCode() + " calling " + uri.getPath(), e);
        } catch (HttpServerErrorException e) {
            throw new ExternalServiceException(
                    "Amadeus HTTP " + e.getStatusCode() + " calling " + uri.getPath(), e);
        } catch (JsonProcessingException e) {
            throw new ExternalServiceException("Invalid JSON from Amadeus", e);
        }
    }

    private String fetchRaw(URI uri, String bearerToken) {
        return amadeusRestClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .retrieve()
                .body(String.class);
    }
}
