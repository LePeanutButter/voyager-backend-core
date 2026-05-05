package com.tourism.platform.controller;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import com.tourism.platform.config.OpenApiConfig;
import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.integration.amadeus.AmadeusCatalogService;
import com.tourism.platform.integration.amadeus.FlightOffersSearchRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Travel catalog lookups backed by Amadeus Self-Service APIs (server-side OAuth2 credentials).
 */
@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
@Validated
@Tag(name = "Travel catalog (Amadeus)", description = "Flights, hotels, and activities via Amadeus integration (JWT required)")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
public class TravelCatalogController {

    static final String MSG_FLIGHT_OK = "Flight offers retrieved from Amadeus";
    static final String MSG_HOTELS_OK = "Hotel list retrieved from Amadeus";
    static final String MSG_OFFERS_OK = "Hotel offers retrieved from Amadeus";
    static final String MSG_ACTIVITIES_OK = "Activities retrieved from Amadeus";

    private final AmadeusCatalogService amadeusCatalogService;

    @GetMapping("/flights")
    @Operation(summary = "Search flight offers",
            description = "Maps to GET /v2/shopping/flight-offers — returns Amadeus JSON as data.")
        /**
         * Proxies flight offer search parameters to Amadeus and returns normalized JSON envelope.
         */
    public ResponseEntity<ApiResponse<JsonNode>> flightOffers(
            HttpServletRequest request,
            @Parameter(description = "Origin IATA airport or city code", example = "MAD")
            @RequestParam @NotBlank @Pattern(regexp = "(?i)^[a-z0-9]{3}$") String originLocationCode,
            @Parameter(description = "Destination IATA airport or city code", example = "BCN")
            @RequestParam @NotBlank @Pattern(regexp = "(?i)^[a-z0-9]{3}$") String destinationLocationCode,
            @Parameter(description = "Departure date (YYYY-MM-DD)", example = "2026-06-01")
            @RequestParam @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") String departureDate,
            @RequestParam(defaultValue = "1") @Min(1) @Max(9) int adults,
            @Parameter(description = "Return date for round-trip (YYYY-MM-DD), optional") @RequestParam(required = false)
            @Pattern(regexp = "^(|\\d{4}-\\d{2}-\\d{2})$") String returnDate,
            @RequestParam(required = false) @Min(0) @Max(9) Integer children,
            @RequestParam(required = false) @Min(1) @Max(250) Integer max,
            @Parameter(description = "ECONOMY, PREMIUM_ECONOMY, BUSINESS or FIRST") @RequestParam(required = false)
            String travelClass,
            @Parameter(description = "Only non-stop itineraries") @RequestParam(required = false) Boolean nonStop,
            @Parameter(description = "ISO currency code for pricing hints", example = "EUR") @RequestParam(required = false)
            @Pattern(regexp = "^(|[A-Za-z]{3})$") String currencyCode) {

        String ret = blankToNull(returnDate);
        String origin = originLocationCode.toUpperCase(Locale.ROOT);
        String dest = destinationLocationCode.toUpperCase(Locale.ROOT);
        JsonNode payload = amadeusCatalogService.flightOffersSearch(new FlightOffersSearchRequest(
                origin,
                dest,
                departureDate,
                adults,
                ret,
                children,
                max == null ? 5 : max,
                blankToNull(travelClass),
                nonStop,
                blankToNull(currencyCode)));

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                MSG_FLIGHT_OK,
                payload,
                request.getRequestURI()));
    }

    @GetMapping("/hotels/by-city")
    @Operation(summary = "List hotels by city",
            description = "Maps to GET /v1/reference-data/locations/hotels/by-city — yields hotel IDs for pricing.")
        /** Returns Amadeus hotel references for cityCode. */
    public ResponseEntity<ApiResponse<JsonNode>> hotelsByCity(
            HttpServletRequest request,
            @Parameter(description = "Amadeus city code (typically 3 letters)", example = "PAR")
            @RequestParam @NotBlank @Pattern(regexp = "(?i)^[a-z]{3}$") String cityCode) {

        JsonNode payload = amadeusCatalogService.hotelsByCity(cityCode.toUpperCase(Locale.ROOT));

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                MSG_HOTELS_OK,
                payload,
                request.getRequestURI()));
    }

    @GetMapping("/hotels/offers")
    @Operation(summary = "Shopping hotel offers",
            description = "Maps to GET /v3/shopping/hotel-offers — pass hotel IDs from /catalog/hotels/by-city.")
        /** Obtains nightly offers for comma-separated hotelIds. */
    public ResponseEntity<ApiResponse<JsonNode>> hotelOffers(
            HttpServletRequest request,
            @Parameter(description = "Comma-separated Amadeus hotel IDs", example = "ADPAR266,ADPAR277")
            @RequestParam @NotBlank @Size(max = 500) String hotelIds,
            @Parameter(example = "2026-06-15") @RequestParam @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
            String checkInDate,
            @Parameter(example = "2026-06-17") @RequestParam @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
            String checkOutDate,
            @RequestParam(defaultValue = "1") @Min(1) @Max(9) int adults,
            @RequestParam(defaultValue = "1") @Min(1) @Max(9) int rooms,
            @Parameter(example = "EUR") @RequestParam(required = false) @Pattern(regexp = "^(|[A-Za-z]{3})$") String currency) {

        JsonNode payload = amadeusCatalogService.hotelOffersSearch(
                hotelIds, checkInDate, checkOutDate, adults, rooms, blankToNull(currency));

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                MSG_OFFERS_OK,
                payload,
                request.getRequestURI()));
    }

    @GetMapping("/activities")
    @Operation(summary = "Nearby activities",
            description = "Maps to GET /v1/shopping/activities — Tours & Activities search by coordinates.")
        /** Geo search for tours and activities returning Amadeus JSON. */
    public ResponseEntity<ApiResponse<JsonNode>> activities(
            HttpServletRequest request,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) double radius,
            @RequestParam(defaultValue = "KM") @Pattern(regexp = "^(KM|MILE)$") String radiusUnit) {

        JsonNode payload =
                amadeusCatalogService.activitiesSearch(latitude, longitude, radius, radiusUnit);

        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK.value(),
                MSG_ACTIVITIES_OK,
                payload,
                request.getRequestURI()));
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
