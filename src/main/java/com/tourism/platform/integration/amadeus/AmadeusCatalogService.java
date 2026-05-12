package com.tourism.platform.integration.amadeus;

import java.net.URI;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import com.tourism.platform.exception.BadRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Travel catalog: {@linkplain AmadeusMockCatalogData Amadeus-compatible mock} by default, or live Amadeus API when
 * {@code mock-mode=false} and credentials are configured.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmadeusCatalogService {

    private final AmadeusProperties properties;
    private final AmadeusMockCatalogData mockCatalogData;
    private final AmadeusApiClient apiClient;

    /**
     * Flight Offers Search GET (same query model as Amadeus v2).
     */
    public JsonNode flightOffersSearch(FlightOffersSearchRequest request) {

        ensureCatalogEnabled();
        if (properties.isMockMode()) {
            log.debug("event=amadeus_catalog_mock op=flightOffers");
            return mockCatalogData.searchFlightOffers(request);
        }

        ensureLiveAmadeusCredentials();

        UriComponentsBuilder ub = UriComponentsBuilder.fromPath("/v2/shopping/flight-offers")
                .queryParam("originLocationCode", request.originLocationCode())
                .queryParam("destinationLocationCode", request.destinationLocationCode())
                .queryParam("departureDate", request.departureDate())
                .queryParam("adults", request.adults());

        if (StringUtils.hasText(request.returnDate())) {
            ub.queryParam("returnDate", request.returnDate());
        }
        if (request.children() != null && request.children() >= 0) {
            ub.queryParam("children", request.children());
        }
        if (request.maxOffers() != null && request.maxOffers() > 0) {
            ub.queryParam("max", Math.min(request.maxOffers(), 250));
        }
        if (StringUtils.hasText(request.travelClass())) {
            ub.queryParam("travelClass", request.travelClass());
        }
        if (Boolean.TRUE.equals(request.nonStop())) {
            ub.queryParam("nonStop", true);
        }
        if (StringUtils.hasText(request.currencyCode())) {
            ub.queryParam("currencyCode", request.currencyCode());
        }

        URI uri = ub.encode().build(true).toUri();
        return apiClient.getJson(uri);
    }

    /** Hotel IDs for a city (Amadeus IATA city code, e.g. PAR, MAD). */
    public JsonNode hotelsByCity(String cityCode) {
        ensureCatalogEnabled();
        if (properties.isMockMode()) {
            log.debug("event=amadeus_catalog_mock op=hotelsByCity");
            return mockCatalogData.hotelsByCity(cityCode);
        }
        ensureLiveAmadeusCredentials();

        URI uri = UriComponentsBuilder.fromPath("/v1/reference-data/locations/hotels/by-city")
                .queryParam("cityCode", cityCode)
                .encode()
                .build(true)
                .toUri();
        return apiClient.getJson(uri);
    }

    /**
     * Shopping hotel offers (requires Amadeus hotel ids from {@link #hotelsByCity}).
     */
    public JsonNode hotelOffersSearch(
            String hotelIdsCsv,
            String checkInDate,
            String checkOutDate,
            int adults,
            int roomQuantity,
            String currencyCode) {

        ensureCatalogEnabled();
        if (properties.isMockMode()) {
            log.debug("event=amadeus_catalog_mock op=hotelOffers");
            return mockCatalogData.hotelOffers(hotelIdsCsv, checkInDate, checkOutDate, adults, roomQuantity, currencyCode);
        }
        ensureLiveAmadeusCredentials();

        UriComponentsBuilder ub = UriComponentsBuilder.fromPath("/v3/shopping/hotel-offers")
                .queryParam("hotelIds", hotelIdsCsv)
                .queryParam("checkInDate", checkInDate)
                .queryParam("checkOutDate", checkOutDate)
                .queryParam("adults", adults)
                .queryParam("roomQuantity", Math.max(1, roomQuantity));
        if (StringUtils.hasText(currencyCode)) {
            ub.queryParam("currency", currencyCode);
        }
        URI uri = ub.encode().build(true).toUri();
        return apiClient.getJson(uri);
    }

    /** Tours &amp; activities around a geo point (Amadeus v1 Shopping). */
    public JsonNode activitiesSearch(double latitude, double longitude, double radius, String radiusUnit) {
        ensureCatalogEnabled();
        if (properties.isMockMode()) {
            log.debug("event=amadeus_catalog_mock op=activities");
            return mockCatalogData.activities(latitude, longitude, radius, radiusUnit);
        }
        ensureLiveAmadeusCredentials();

        String ru = StringUtils.hasText(radiusUnit) ? radiusUnit : "KM";

        URI uri = UriComponentsBuilder.fromPath("/v1/shopping/activities")
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("radius", radius <= 0 ? 1 : radius)
                .queryParam("radiusUnit", ru)
                .encode()
                .build(true)
                .toUri();
        return apiClient.getJson(uri);
    }

    private void ensureCatalogEnabled() {
        if (!properties.isEnabled()) {
            throw new BadRequestException(
                    "Travel catalog is disabled. Set app.integrations.amadeus.enabled=true (AMADEUS_CATALOG_ENABLED).");
        }
    }

    private void ensureLiveAmadeusCredentials() {
        if (!StringUtils.hasText(properties.getClientId()) || !StringUtils.hasText(properties.getClientSecret())) {
            throw new BadRequestException(
                    "Live Amadeus requires credentials. Set AMADEUS_CLIENT_ID / AMADEUS_CLIENT_SECRET, "
                            + "or keep AMADEUS_MOCK_MODE=true for demo data.");
        }
    }
}
