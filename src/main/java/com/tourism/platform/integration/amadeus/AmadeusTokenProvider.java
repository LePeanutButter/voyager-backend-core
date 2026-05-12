package com.tourism.platform.integration.amadeus;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.tourism.platform.exception.ExternalServiceException;

import lombok.extern.slf4j.Slf4j;

/**
 * Fetches and caches Amadeus OAuth2 access tokens (client_credentials).
 */
@Component
@Slf4j
public class AmadeusTokenProvider {

    private final AmadeusProperties properties;
    private final RestClient amadeusRestClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private final ReentrantLock lock = new ReentrantLock();
    private volatile String cachedToken;
    private volatile Instant expiry = Instant.EPOCH;

    public AmadeusTokenProvider(
            AmadeusProperties properties,
            RestClient amadeusRestClient,
            ObjectMapper objectMapper,
            Clock clock) {
        this.properties = properties;
        this.amadeusRestClient = amadeusRestClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * @return bearer access token valid for outbound Amadeus API calls.
     */
    public String getAccessToken() {
        Instant now = clock.instant();
        if (cachedToken != null && now.isBefore(expiry.minusSeconds(60))) {
            return cachedToken;
        }

        lock.lock();
        try {
            now = clock.instant();
            if (cachedToken != null && now.isBefore(expiry.minusSeconds(60))) {
                return cachedToken;
            }
            refreshTokenLocked(now);
            return cachedToken;
        } finally {
            lock.unlock();
        }
    }

    private void refreshTokenLocked(Instant now) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        String body = amadeusRestClient.post()
                .uri(properties.getTokenPath())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), (req, res) -> {
                    String err = res.getStatusCode() + " token request failed";
                    log.warn("event=amadeus_token_error status={}", res.getStatusCode());
                    throw new ExternalServiceException(err);
                })
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(body.getBytes(StandardCharsets.UTF_8));
            if (!root.hasNonNull("access_token")) {
                throw new ExternalServiceException("Amadeus token response missing access_token");
            }
            cachedToken = root.get("access_token").asText();
            long seconds = root.path("expires_in").asLong(1_800);
            expiry = now.plusSeconds(Math.max(60, seconds));
            log.debug("event=amadeus_token_refreshed expiresInSeconds={}", seconds);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Failed to parse Amadeus token response", e);
        }
    }

    /**
     * Clears cache (e.g. after a 401 from the API).
     */
    public void invalidate() {
        lock.lock();
        try {
            cachedToken = null;
            expiry = Instant.EPOCH;
        } finally {
            lock.unlock();
        }
    }
}
