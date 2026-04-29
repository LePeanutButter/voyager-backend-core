package com.tourism.platform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourism.platform.config.GoogleOAuthProperties;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.exception.ExternalServiceException;
import com.tourism.platform.model.User;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.security.JwtTokenProvider;
import com.tourism.platform.service.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final GoogleOAuthProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public UserDto authenticateWithAuthorizationCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Authorization code is required");
        }

        String accessToken = exchangeCodeForAccessToken(code);
        GoogleProfile profile = fetchGoogleProfile(accessToken);

        User user = findOrCreateGoogleUser(profile);

        UserDto dto = toDto(user);
        dto.setToken(jwtTokenProvider.generateTokenFromUsernameAndUserId(dto.getUsername(), user.getId()));
        return dto;
    }

    private String exchangeCodeForAccessToken(String code) {
        if (properties.getClientId() == null || properties.getClientId().isBlank()) {
            throw new BusinessException("Google client-id is not configured");
        }
        if (properties.getClientSecret() == null || properties.getClientSecret().isBlank()) {
            throw new BusinessException("Google client-secret is not configured");
        }

        String tokenUrl = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ExternalServiceException("Google token exchange failed");
            }

            JsonNode node = objectMapper.readTree(response.getBody());
            JsonNode accessToken = node.get("access_token");
            if (accessToken == null || accessToken.asText().isBlank()) {
                throw new ExternalServiceException("Google token response missing access_token");
            }
            return accessToken.asText();
        } catch (RestClientException e) {
            throw new ExternalServiceException("Google token exchange failed: " + e.getMessage());
        } catch (Exception e) {
            throw new ExternalServiceException("Google token exchange parse failed: " + e.getMessage());
        }
    }

    private GoogleProfile fetchGoogleProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v2/userinfo",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ExternalServiceException("Google userinfo request failed");
            }

            JsonNode node = objectMapper.readTree(response.getBody());
            String email = textOrNull(node, "email");
            String name = textOrNull(node, "name");

            if (email == null || email.isBlank()) {
                throw new ExternalServiceException("Google userinfo missing email");
            }

            return new GoogleProfile(email, name);
        } catch (RestClientException e) {
            throw new ExternalServiceException("Google userinfo request failed: " + e.getMessage());
        } catch (Exception e) {
            throw new ExternalServiceException("Google userinfo parse failed: " + e.getMessage());
        }
    }

    private User findOrCreateGoogleUser(GoogleProfile profile) {
        Optional<User> existing = userRepository.findByEmail(profile.email());
        if (existing.isPresent()) {
            return existing.get();
        }

        String baseUsername = profile.email().split("@")[0];
        String username = ensureUniqueUsername(baseUsername);

        User user = new User();
        user.setUsername(username);
        user.setEmail(profile.email());

        // Keep current constraints happy (password is required), but Google users won't use password login.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        String[] names = splitName(profile.name());
        user.setFirstName(names[0]);
        user.setLastName(names[1]);
        user.setPhoneNumber(null);
        user.setRole(UserRole.TRAVELER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        user.setGoogleUser(true);
        user.setInterests(Set.of());

        return userRepository.save(user);
    }

    private String ensureUniqueUsername(String base) {
        String candidate = sanitizeUsername(base);
        if (candidate.isBlank()) {
            candidate = "google_user";
        }
        if (!userRepository.existsByUsername(candidate)) {
            return candidate;
        }
        for (int i = 1; i <= 1000; i++) {
            String attempt = candidate + i;
            if (!userRepository.existsByUsername(attempt)) {
                return attempt;
            }
        }
        return candidate + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String sanitizeUsername(String value) {
        return value == null ? "" : value.trim().replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }

    private String[] splitName(String name) {
        String safe = (name == null || name.isBlank()) ? "Google User" : name.trim();
        String[] parts = safe.split("\\s+");
        String first = parts.length > 0 ? parts[0] : "Google";
        String last = parts.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)) : "User";
        return new String[]{first, last};
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    @SuppressWarnings("unused")
    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setBio(user.getBio());
        if (user.getInterests() != null) {
            dto.setInterests(new java.util.HashSet<>(user.getInterests()));
        }
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    private record GoogleProfile(String email, String name) {}
}

