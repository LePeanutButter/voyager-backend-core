package com.tourism.platform.integration.amadeus;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.tourism.platform.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class AmadeusCatalogServiceTest {

    @Mock
    private AmadeusProperties properties;

    @Mock
    private AmadeusMockCatalogData mockData;

    @Mock
    private AmadeusApiClient apiClient;

    private AmadeusCatalogService service;

    @BeforeEach
    void setUp() {
        service = new AmadeusCatalogService(properties, mockData, apiClient);
    }

    @Test
    void whenCatalogDisabled_thenBadRequest() {
        when(properties.isEnabled()).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.hotelsByCity("PAR"));
    }

    @Test
    void whenMockMode_thenUsesMockWithoutApiClient() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(true);
        ObjectNode node = new ObjectMapper().createObjectNode();
        when(mockData.hotelsByCity("PAR")).thenReturn(node);

        assertSame(node, service.hotelsByCity("PAR"));

        verify(mockData).hotelsByCity("PAR");
        verifyNoInteractions(apiClient);
    }

    @Test
    void whenLiveModeWithoutSecrets_thenBadRequest() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(false);
        when(properties.getClientId()).thenReturn("");
        assertThrows(BadRequestException.class, () -> service.hotelsByCity("PAR"));
    }

    @Test
    void whenLiveMode_flightOffers_buildsUriWithOptionalParams() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(false);
        when(properties.getClientId()).thenReturn("id");
        when(properties.getClientSecret()).thenReturn("sec");
        ObjectNode api = new ObjectMapper().createObjectNode().put("x", 1);
        when(apiClient.getJson(any())).thenReturn(api);

        FlightOffersSearchRequest req = new FlightOffersSearchRequest(
                "mad",
                "bcn",
                "2026-06-01",
                2,
                "2026-06-10",
                1,
                300,
                "BUSINESS",
                true,
                "usd");

        assertSame(api, service.flightOffersSearch(req));

        ArgumentCaptor<URI> cap = ArgumentCaptor.forClass(URI.class);
        verify(apiClient).getJson(cap.capture());
        String u = cap.getValue().toASCIIString();
        org.junit.jupiter.api.Assertions.assertTrue(u.contains("originLocationCode=mad"));
        org.junit.jupiter.api.Assertions.assertTrue(u.contains("destinationLocationCode=bcn"));
        org.junit.jupiter.api.Assertions.assertTrue(u.contains("departureDate=2026-06-01"));
        org.junit.jupiter.api.Assertions.assertTrue(u.contains("adults=2"));
        org.junit.jupiter.api.Assertions.assertTrue(u.contains("returnDate=2026-06-10"));
        org.junit.jupiter.api.Assertions.assertTrue(u.contains("children=1"));
        org.junit.jupiter.api.Assertions.assertTrue(u.contains("max=250"));
        org.junit.jupiter.api.Assertions.assertTrue(u.contains("travelClass=BUSINESS"));
        org.junit.jupiter.api.Assertions.assertTrue(u.contains("nonStop=true"));
        org.junit.jupiter.api.Assertions.assertTrue(u.contains("currencyCode=usd"));
    }

    @Test
    void whenLiveMode_flightOffers_skipsOptionalQueryParamsWhenAbsent() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(false);
        when(properties.getClientId()).thenReturn("id");
        when(properties.getClientSecret()).thenReturn("sec");
        when(apiClient.getJson(any())).thenReturn(new ObjectMapper().createObjectNode());

        FlightOffersSearchRequest req =
                new FlightOffersSearchRequest("MAD", "BCN", "2026-06-01", 1, null, null, 0, null, null, null);

        service.flightOffersSearch(req);

        verify(apiClient)
                .getJson(argThat(uri -> {
                    String u = uri.toASCIIString();
                    return !u.contains("returnDate")
                            && !u.contains("children=")
                            && !u.contains("max=")
                            && !u.contains("travelClass")
                            && !u.contains("nonStop")
                            && !u.contains("currencyCode");
                }));
    }

    @Test
    void whenLiveMode_hotelOffers_addsCurrencyWhenPresent() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(false);
        when(properties.getClientId()).thenReturn("id");
        when(properties.getClientSecret()).thenReturn("sec");
        when(apiClient.getJson(any())).thenReturn(new ObjectMapper().createObjectNode());

        service.hotelOffersSearch("H1,H2", "2026-06-01", "2026-06-03", 2, 2, "GBP");

        verify(apiClient).getJson(argThat(uri -> uri.toASCIIString().contains("currency=GBP")));
    }

    @Test
    void whenLiveMode_hotelOffers_omitsCurrencyWhenBlank() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(false);
        when(properties.getClientId()).thenReturn("id");
        when(properties.getClientSecret()).thenReturn("sec");
        when(apiClient.getJson(any())).thenReturn(new ObjectMapper().createObjectNode());

        service.hotelOffersSearch("H1", "2026-06-01", "2026-06-03", 1, 1, "  ");

        verify(apiClient).getJson(argThat(uri -> !uri.toASCIIString().contains("currency=")));
    }

    @Test
    void whenLiveMode_activities_usesRadiusOneWhenNonPositive_andKeepsRadiusUnit() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(false);
        when(properties.getClientId()).thenReturn("id");
        when(properties.getClientSecret()).thenReturn("sec");
        when(apiClient.getJson(any())).thenReturn(new ObjectMapper().createObjectNode());

        service.activitiesSearch(1.0, 2.0, 0, "MILE");

        verify(apiClient).getJson(argThat(uri -> {
            String u = uri.toASCIIString();
            return u.contains("radius=1") && u.contains("radiusUnit=MILE");
        }));
    }

    @Test
    void whenLiveMode_activities_blankRadiusUnit_defaultsToKm() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(false);
        when(properties.getClientId()).thenReturn("id");
        when(properties.getClientSecret()).thenReturn("sec");
        when(apiClient.getJson(any())).thenReturn(new ObjectMapper().createObjectNode());

        service.activitiesSearch(1.0, 2.0, 5, "   ");

        verify(apiClient).getJson(argThat(uri -> uri.toASCIIString().contains("radiusUnit=KM")));
    }

    @Test
    void whenMockMode_flightHotelActivities_delegateToMock() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(true);
        ObjectMapper om = new ObjectMapper();
        ObjectNode f = om.createObjectNode();
        ObjectNode h = om.createObjectNode();
        ObjectNode o = om.createObjectNode();
        ObjectNode a = om.createObjectNode();
        when(mockData.searchFlightOffers(any())).thenReturn(f);
        when(mockData.hotelOffers(any(), any(), any(), anyInt(), anyInt(), any())).thenReturn(o);
        when(mockData.activities(anyDouble(), anyDouble(), anyDouble(), any())).thenReturn(a);

        FlightOffersSearchRequest req =
                new FlightOffersSearchRequest("MAD", "BCN", "2026-06-01", 1, null, null, 5, null, null, null);
        assertSame(f, service.flightOffersSearch(req));
        when(mockData.hotelsByCity("LON")).thenReturn(h);
        assertSame(h, service.hotelsByCity("LON"));
        assertSame(
                o,
                service.hotelOffersSearch("X", "2026-06-01", "2026-06-02", 1, 1, null));
        assertSame(a, service.activitiesSearch(1, 2, 3, "KM"));

        verifyNoInteractions(apiClient);
    }

    @Test
    void whenCatalogDisabled_flightOffers_throwsBeforeMock() {
        when(properties.isEnabled()).thenReturn(false);
        FlightOffersSearchRequest req =
                new FlightOffersSearchRequest("MAD", "BCN", "2026-06-01", 1, null, null, 5, null, null, null);
        assertThrows(BadRequestException.class, () -> service.flightOffersSearch(req));
    }

    @Test
    void whenLiveMode_hotelsByCity_passesCityCodeToQueryString() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(false);
        when(properties.getClientId()).thenReturn("id");
        when(properties.getClientSecret()).thenReturn("sec");
        ObjectNode node = new ObjectMapper().createObjectNode();
        when(apiClient.getJson(any())).thenReturn(node);

        assertSame(node, service.hotelsByCity("par"));

        verify(apiClient).getJson(argThat(uri -> uri.toASCIIString().contains("cityCode=par")));
    }

    @Test
    void whenLiveMode_flightOffers_negativeChildren_skipsChildrenParam() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(false);
        when(properties.getClientId()).thenReturn("id");
        when(properties.getClientSecret()).thenReturn("sec");
        when(apiClient.getJson(any())).thenReturn(new ObjectMapper().createObjectNode());

        FlightOffersSearchRequest req =
                new FlightOffersSearchRequest("MAD", "BCN", "2026-06-01", 1, null, -1, 5, null, null, null);
        service.flightOffersSearch(req);

        verify(apiClient).getJson(argThat(uri -> !uri.toASCIIString().contains("children=")));
    }

    @Test
    void whenLiveMode_onlyClientSecretMissing_throws() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isMockMode()).thenReturn(false);
        when(properties.getClientId()).thenReturn("id");
        when(properties.getClientSecret()).thenReturn("  ");
        assertThrows(BadRequestException.class, () -> service.activitiesSearch(0, 0, 1, "KM"));
    }
}
