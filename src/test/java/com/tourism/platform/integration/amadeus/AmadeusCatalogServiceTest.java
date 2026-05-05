package com.tourism.platform.integration.amadeus;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

        service.hotelsByCity("PAR");

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
}
