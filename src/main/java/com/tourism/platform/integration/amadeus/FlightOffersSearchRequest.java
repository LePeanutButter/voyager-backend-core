package com.tourism.platform.integration.amadeus;

/**
 * Parameters for Amadeus Flight Offers Search (GET /v2/shopping/flight-offers).
 */
public record FlightOffersSearchRequest(
        String originLocationCode,
        String destinationLocationCode,
        String departureDate,
        int adults,
        String returnDate,
        Integer children,
        Integer maxOffers,
        String travelClass,
        Boolean nonStop,
        String currencyCode) {}
