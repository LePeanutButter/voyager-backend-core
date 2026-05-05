package com.tourism.platform.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.tourism.platform.integration.amadeus.AmadeusCatalogService;
import com.tourism.platform.integration.amadeus.FlightOffersSearchRequest;

@ExtendWith(MockitoExtension.class)
class TravelCatalogControllerTest {

    @Mock
    private AmadeusCatalogService amadeusCatalogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc =
                MockMvcBuilders.standaloneSetup(new TravelCatalogController(amadeusCatalogService))
                        .setValidator(validator)
                        .build();
    }

    @Test
    void flightOffers_ReturnsEnvelope() throws Exception {
        var node = new ObjectMapper().readTree("{\"meta\":{\"count\":0},\"data\":[]}");
        when(amadeusCatalogService.flightOffersSearch(
                        eq(new FlightOffersSearchRequest(
                                "MAD",
                                "BCN",
                                "2026-06-01",
                                1,
                                null,
                                null,
                                5,
                                null,
                                null,
                                null))))
                .thenReturn(node);

        mockMvc.perform(get("/catalog/flights")
                        .param("originLocationCode", "mad")
                        .param("destinationLocationCode", "bcn")
                        .param("departureDate", "2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(TravelCatalogController.MSG_FLIGHT_OK))
                .andExpect(jsonPath("$.data.data").isArray());
    }

    @Test
    void hotelsByCity_PassesUppercaseCity() throws Exception {
        var node = new ObjectMapper().readTree("{\"data\":[]}");
        when(amadeusCatalogService.hotelsByCity("PAR")).thenReturn(node);

        mockMvc.perform(get("/catalog/hotels/by-city").param("cityCode", "par"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(TravelCatalogController.MSG_HOTELS_OK));

        verify(amadeusCatalogService).hotelsByCity("PAR");
    }

    @Test
    void activities_UsesDefaults() throws Exception {
        var node = new ObjectMapper().readTree("{\"data\":[]}");
        when(amadeusCatalogService.activitiesSearch(eq(41.4), eq(2.16), eq(5.0), eq("KM")))
                .thenReturn(node);

        mockMvc.perform(get("/catalog/activities")
                        .param("latitude", "41.4")
                        .param("longitude", "2.16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(TravelCatalogController.MSG_ACTIVITIES_OK));
    }

    @Test
    void hotelOffers_missingHotelIds_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/catalog/hotels/offers")
                        .param("checkInDate", "2026-06-15")
                        .param("checkOutDate", "2026-06-17"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hotelOffers_CallsService() throws Exception {
        var node = new ObjectMapper().readTree("{\"data\":[]}");
        when(amadeusCatalogService.hotelOffersSearch(eq("H1,H2"), eq("2026-06-15"), eq("2026-06-17"), eq(2), eq(1), isNull()))
                .thenReturn(node);

        mockMvc.perform(get("/catalog/hotels/offers")
                        .param("hotelIds", "H1,H2")
                        .param("checkInDate", "2026-06-15")
                        .param("checkOutDate", "2026-06-17")
                        .param("adults", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(TravelCatalogController.MSG_OFFERS_OK));
    }
}
