package com.tourism.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for exchanging a Google server authorization code (e.g. Android
 * {@code serverAuthCode} from Play Services with {@code requestServerAuthCode(webClientId)}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Google server auth code from a native client")
public class GoogleServerAuthCodeRequest {

    @NotBlank
    @Schema(description = "OAuth2 authorization code from Google (serverAuthCode on Android)", example = "4/0A...")
    private String code;
}
