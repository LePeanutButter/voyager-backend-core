package com.tourism.platform.integration.amadeus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class AmadeusMockCatalogDataTest {

    private static AmadeusMockCatalogData data;
    private static String sampleOrig;
    private static String sampleDest;
    private static String sampleDay;

    @BeforeAll
    static void initCatalogAndSampleRoute() {
        data = new AmadeusMockCatalogData(new ObjectMapper());
        LocalDate start = LocalDate.parse(AmadeusMockCatalogData.MOCK_WINDOW_START);
        LocalDate end = LocalDate.parse(AmadeusMockCatalogData.MOCK_WINDOW_END);
        List<String> hubs = List.of(
                "MAD", "BCN", "PAR", "LON", "LIS", "FCO", "AMS", "BER", "VIE", "ZRH", "NYC", "MIA");

        outer:
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            String day = d.toString();
            for (String o : hubs) {
                for (String dest : hubs) {
                    if (o.equals(dest)) {
                        continue;
                    }
                    ObjectNode r = data.searchFlightOffers(
                            new FlightOffersSearchRequest(o, dest, day, 1, null, null, 50, null, null, null));
                    if (r.path("data").size() > 0) {
                        sampleOrig = o;
                        sampleDest = dest;
                        sampleDay = day;
                        break outer;
                    }
                }
            }
        }
    }

    @Test
    void totalMockRecords_atLeast500() {
        assertTrue(data.totalMockRecords() >= 500);
    }

    @Test
    void flightWindow_matchesReadmeExpectation() {
        assertEquals("2026-05-25", AmadeusMockCatalogData.MOCK_WINDOW_START);
        assertEquals("2027-01-31", AmadeusMockCatalogData.MOCK_WINDOW_END);
    }

    @Test
    void searchFlightOffers_primaryMatch_returnsDataAndMeta() {
        assertNotNull(sampleDay, "mock should contain at least one flight in the hub/date window");

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 2, null, 2, 10, null, null, "usd"));

        assertTrue(root.path("data").isArray());
        assertTrue(root.path("data").size() > 0);
        assertEquals("USD", root.path("data").get(0).path("price").path("currency").asText());
        assertTrue(root.path("meta").path("count").isIntegralNumber());
        assertTrue(root.path("dictionaries").isObject());
    }

    @Test
    void searchFlightOffers_withReturnDate_setsOneWayFalseAndSecondItinerary() {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, "2026-07-01", null, 5, null, null, null));

        JsonNode first = root.path("data").get(0);
        assertNotNull(first);
        assertFalse(first.path("oneWay").asBoolean());
        assertTrue(first.path("itineraries").size() >= 2);
    }

    @Test
    void searchFlightOffers_strictCabinUsesFallbackStillReturnsRows() {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 15, "FIRST", null, null));

        assertTrue(
                root.path("data").size() > 0,
                "when primary cabin filter is empty, fallback still fills by O/D segment");
    }

    @Test
    void searchFlightOffers_nonStopTrue_runsSuccessfully() {
        assertNotNull(sampleDay);

        ObjectNode withStopFilter = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 50, null, true, null));
        assertTrue(withStopFilter.path("data").isArray());
    }

    @Test
    void searchFlightOffers_maxOffersNull_usesInternalCap() {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, null, null, null, null));
        assertTrue(root.path("data").size() <= 50);
    }

    @Test
    void searchFlightOffers_economyTravelClass_runsSuccessfully() {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 10, "ECONOMY", null, null));
        assertTrue(root.path("data").size() > 0);
    }

    @Test
    void hotelsByCity_knownCity_returnsOnlyThatCityRows() {
        ObjectNode root = data.hotelsByCity("PAR");
        ArrayNode rows = (ArrayNode) root.path("data");
        assertTrue(rows.size() > 0);
        for (JsonNode row : rows) {
            assertEquals("PAR", row.path("iataCode").asText());
        }
    }

    @Test
    void hotelsByCity_unknownCity_returnsEmptyData() {
        ObjectNode root = data.hotelsByCity("ZZZ");
        assertEquals(0, root.path("data").size());
    }

    @Test
    void hotelOffers_knownIds_buildsOffers_withEurAndUsdBranches() {
        ObjectNode byCity = data.hotelsByCity("PAR");
        String id0 = byCity.path("data").get(0).path("hotelId").asText();
        String id1 = byCity.path("data").get(1).path("hotelId").asText();

        ObjectNode eur = data.hotelOffers(id0 + "," + id1, "2026-06-10", "2026-06-14", 2, 2, null);
        assertEquals(2, eur.path("data").size());

        ObjectNode usd = data.hotelOffers(id0, "2026-06-10", "2026-06-14", 1, 1, "usd");
        assertEquals("USD", usd.path("data").get(0).path("offers").get(0).path("price").path("currency").asText());
    }

    @Test
    void hotelOffers_unknownId_skippedWithoutError() {
        ObjectNode root = data.hotelOffers("UNKNOWNHOTEL", "2026-06-10", "2026-06-12", 1, 1, "EUR");
        assertEquals(0, root.path("data").size());
    }

    @Test
    void activities_radiusZero_usesDefaultFiveKm_andNullUnitShowsKmInMeta() {
        ObjectNode root = data.activities(48.8566, 2.3522, 0, null);
        assertEquals(5.0, root.path("meta").path("radius").asDouble());
        assertEquals("KM", root.path("meta").path("radiusUnit").asText());
        assertTrue(root.path("data").isArray());
    }

    @Test
    void activities_mileUnit_preservedInMeta() {
        ObjectNode root = data.activities(48.8566, 2.3522, 10, "MILE");
        assertEquals("MILE", root.path("meta").path("radiusUnit").asText());
    }

    @Test
    void activities_parisCenter_returnsSomeRows() {
        ObjectNode root = data.activities(48.8566, 2.3522, 500, "KM");
        assertTrue(root.path("data").size() > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ECONOMY", "BUSINESS", "PREMIUM_ECONOMY", "FIRST"})
    void searchFlightOffers_variousTravelClasses_runs(String travelClass) {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 20, travelClass, null, null));
        assertTrue(root.path("data").isArray());
    }

    @Test
    void searchFlightOffers_blankTravelClass_skipsCabinFilter() {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 20, "   ", null, null));
        assertTrue(root.path("data").size() > 0);
    }

    @Test
    void searchFlightOffers_nonStopFalse_explicit() {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 25, null, Boolean.FALSE, null));
        assertTrue(root.path("data").isArray());
    }

    @Test
    void searchFlightOffers_maxOffersAtUpperBound_capsAt250() {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 500, null, null, null));
        assertTrue(root.path("data").size() <= 250);
    }

    @Test
    void searchFlightOffers_maxOffersOne_returnsAtMostOne() {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 1, null, null, null));
        assertTrue(root.path("data").size() <= 1);
    }

    @Test
    void searchFlightOffers_blankCurrency_defaultsToEurInPayload() {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 5, null, null, "   "));
        assertEquals("EUR", root.path("data").get(0).path("price").path("currency").asText());
    }

    @Test
    void hotelOffers_zeroRooms_usesAtLeastOneForPricing() {
        ObjectNode byCity = data.hotelsByCity("MAD");
        String hid = byCity.path("data").get(0).path("hotelId").asText();

        ObjectNode root = data.hotelOffers(hid, "2026-06-10", "2026-06-12", 2, 0, "EUR");
        assertEquals(1, root.path("data").size());
        assertFalse(
                root.path("data").get(0).path("offers").get(0).path("price").path("total").asText().isEmpty());
    }

    @Test
    void hotelOffers_sameCheckInAndOut_usesSingleNight() {
        ObjectNode byCity = data.hotelsByCity("BER");
        String hid = byCity.path("data").get(0).path("hotelId").asText();

        ObjectNode root = data.hotelOffers(hid, "2026-08-01", "2026-08-01", 1, 1, "EUR");
        assertEquals(1, root.path("data").size());
    }

    @Test
    void hotelOffers_trimmedCommaSeparatedIds() {
        ObjectNode byCity = data.hotelsByCity("LON");
        String id0 = byCity.path("data").get(0).path("hotelId").asText();
        String id1 = byCity.path("data").get(1).path("hotelId").asText();

        ObjectNode root = data.hotelOffers("  " + id0 + " , " + id1 + "  ", "2026-06-10", "2026-06-12", 1, 1, "EUR");
        assertEquals(2, root.path("data").size());
    }

    @Test
    void searchFlightOffers_withChildren_adjustsPriceMultiplier() {
        assertNotNull(sampleDay);

        ObjectNode without = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 3, null, null, null));
        ObjectNode withChild = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, 1, 3, null, null, null));

        double t0 = without.path("data").get(0).path("price").path("total").asDouble();
        double t1 = withChild.path("data").get(0).path("price").path("total").asDouble();
        assertTrue(t1 > t0);
    }

    @Test
    void searchFlightOffers_eurCurrency_keepsEurBranchInPriceAdjust() {
        assertNotNull(sampleDay);

        ObjectNode root = data.searchFlightOffers(new FlightOffersSearchRequest(
                sampleOrig, sampleDest, sampleDay, 1, null, null, 3, null, null, "EUR"));
        assertEquals("EUR", root.path("data").get(0).path("price").path("currency").asText());
    }
}
