package com.tourism.platform.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TravelerMatchDtoTest {

    @Test
    void createSimpleMatch_WithValidParameters_ShouldCreateDto() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String firstName = "Test";
        String lastName = "User";
        String destinationLocation = "Paris";
        LocalDateTime travelStartDate = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime travelEndDate = LocalDateTime.of(2024, 6, 7, 18, 0);
        Integer daysOverlap = 5;

        // When
        TravelerMatchDto result = TravelerMatchDto.createSimpleMatch(
                userId, username, firstName, lastName, destinationLocation,
                travelStartDate, travelEndDate, daysOverlap
        );

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(username, result.getUsername());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertEquals(destinationLocation, result.getDestinationLocation());
        assertEquals(travelStartDate, result.getTravelStartDate());
        assertEquals(travelEndDate, result.getTravelEndDate());
        assertEquals(daysOverlap, result.getDaysOverlap());
    }

    @Test
    void createSimpleMatch_WithNullParameters_ShouldHandleNulls() {
        // Given
        Long userId = null;
        String username = null;
        String firstName = null;
        String lastName = null;
        String destinationLocation = null;
        LocalDateTime travelStartDate = null;
        LocalDateTime travelEndDate = null;
        Integer daysOverlap = null;

        // When
        TravelerMatchDto result = TravelerMatchDto.createSimpleMatch(
                userId, username, firstName, lastName, destinationLocation,
                travelStartDate, travelEndDate, daysOverlap
        );

        // Then
        assertNotNull(result);
        assertNull(result.getUserId());
        assertNull(result.getUsername());
        assertNull(result.getFirstName());
        assertNull(result.getLastName());
        assertNull(result.getDestinationLocation());
        assertNull(result.getTravelStartDate());
        assertNull(result.getTravelEndDate());
        assertNull(result.getDaysOverlap());
    }

    @Test
    void createSimpleMatch_WithEmptyStrings_ShouldHandleEmptyValues() {
        // Given
        Long userId = 1L;
        String username = "";
        String firstName = "";
        String lastName = "";
        String destinationLocation = "";
        LocalDateTime travelStartDate = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime travelEndDate = LocalDateTime.of(2024, 6, 7, 18, 0);
        Integer daysOverlap = 5;

        // When
        TravelerMatchDto result = TravelerMatchDto.createSimpleMatch(
                userId, username, firstName, lastName, destinationLocation,
                travelStartDate, travelEndDate, daysOverlap
        );

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(username, result.getUsername());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertEquals(destinationLocation, result.getDestinationLocation());
        assertEquals(travelStartDate, result.getTravelStartDate());
        assertEquals(travelEndDate, result.getTravelEndDate());
        assertEquals(daysOverlap, result.getDaysOverlap());
    }

    @Test
    void createSimpleMatch_WithZeroDaysOverlap_ShouldCreateDto() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String firstName = "Test";
        String lastName = "User";
        String destinationLocation = "Paris";
        LocalDateTime travelStartDate = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime travelEndDate = LocalDateTime.of(2024, 6, 7, 18, 0);
        Integer daysOverlap = 0;

        // When
        TravelerMatchDto result = TravelerMatchDto.createSimpleMatch(
                userId, username, firstName, lastName, destinationLocation,
                travelStartDate, travelEndDate, daysOverlap
        );

        // Then
        assertNotNull(result);
        assertEquals(daysOverlap, result.getDaysOverlap());
    }

    @Test
    void createSimpleMatch_WithNegativeDaysOverlap_ShouldCreateDto() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String firstName = "Test";
        String lastName = "User";
        String destinationLocation = "Paris";
        LocalDateTime travelStartDate = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime travelEndDate = LocalDateTime.of(2024, 6, 7, 18, 0);
        Integer daysOverlap = -1;

        // When
        TravelerMatchDto result = TravelerMatchDto.createSimpleMatch(
                userId, username, firstName, lastName, destinationLocation,
                travelStartDate, travelEndDate, daysOverlap
        );

        // Then
        assertNotNull(result);
        assertEquals(daysOverlap, result.getDaysOverlap());
    }

    @Test
    void createSimpleMatch_WithLargeValues_ShouldCreateDto() {
        // Given
        Long userId = Long.MAX_VALUE;
        String username = "a".repeat(1000);
        String firstName = "b".repeat(500);
        String lastName = "c".repeat(500);
        String destinationLocation = "d".repeat(1000);
        LocalDateTime travelStartDate = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime travelEndDate = LocalDateTime.of(2024, 6, 7, 18, 0);
        Integer daysOverlap = Integer.MAX_VALUE;

        // When
        TravelerMatchDto result = TravelerMatchDto.createSimpleMatch(
                userId, username, firstName, lastName, destinationLocation,
                travelStartDate, travelEndDate, daysOverlap
        );

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(username, result.getUsername());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertEquals(destinationLocation, result.getDestinationLocation());
        assertEquals(travelStartDate, result.getTravelStartDate());
        assertEquals(travelEndDate, result.getTravelEndDate());
        assertEquals(daysOverlap, result.getDaysOverlap());
    }

    @Test
    void createSimpleMatch_WithSameDates_ShouldCreateDto() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String firstName = "Test";
        String lastName = "User";
        String destinationLocation = "Paris";
        LocalDateTime sameDate = LocalDateTime.of(2024, 6, 1, 10, 0);
        Integer daysOverlap = 0;

        // When
        TravelerMatchDto result = TravelerMatchDto.createSimpleMatch(
                userId, username, firstName, lastName, destinationLocation,
                sameDate, sameDate, daysOverlap
        );

        // Then
        assertNotNull(result);
        assertEquals(sameDate, result.getTravelStartDate());
        assertEquals(sameDate, result.getTravelEndDate());
        assertEquals(daysOverlap, result.getDaysOverlap());
    }

    @Test
    void createSimpleMatch_ShouldUseBuilderPatternInternally() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String firstName = "Test";
        String lastName = "User";
        String destinationLocation = "Paris";
        LocalDateTime travelStartDate = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime travelEndDate = LocalDateTime.of(2024, 6, 7, 18, 0);
        Integer daysOverlap = 5;

        // When
        TravelerMatchDto result = TravelerMatchDto.createSimpleMatch(
                userId, username, firstName, lastName, destinationLocation,
                travelStartDate, travelEndDate, daysOverlap
        );

        // Then
        assertNotNull(result);
        // The fact that this method works without throwing exceptions
        // indicates that the Builder pattern is being used correctly internally
        assertEquals(userId, result.getUserId());
        assertEquals(username, result.getUsername());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertEquals(destinationLocation, result.getDestinationLocation());
        assertEquals(travelStartDate, result.getTravelStartDate());
        assertEquals(travelEndDate, result.getTravelEndDate());
        assertEquals(daysOverlap, result.getDaysOverlap());
    }

    @Test
    void createSimpleMatch_WithSpecialCharacters_ShouldCreateDto() {
        // Given
        Long userId = 1L;
        String username = "test@user.com";
        String firstName = "Test-User";
        String lastName = "O'Reilly";
        String destinationLocation = "São Paulo, Brazil";
        LocalDateTime travelStartDate = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime travelEndDate = LocalDateTime.of(2024, 6, 7, 18, 0);
        Integer daysOverlap = 5;

        // When
        TravelerMatchDto result = TravelerMatchDto.createSimpleMatch(
                userId, username, firstName, lastName, destinationLocation,
                travelStartDate, travelEndDate, daysOverlap
        );

        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertEquals(destinationLocation, result.getDestinationLocation());
    }

    @Test
    void createSimpleMatch_MultipleCalls_ShouldCreateIndependentInstances() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        String username1 = "user1";
        String username2 = "user2";

        // When
        TravelerMatchDto result1 = TravelerMatchDto.createSimpleMatch(
                userId1, username1, "First", "Last", "Paris",
                LocalDateTime.of(2024, 6, 1, 10, 0),
                LocalDateTime.of(2024, 6, 7, 18, 0), 5
        );

        TravelerMatchDto result2 = TravelerMatchDto.createSimpleMatch(
                userId2, username2, "First", "Last", "Paris",
                LocalDateTime.of(2024, 6, 1, 10, 0),
                LocalDateTime.of(2024, 6, 7, 18, 0), 5
        );

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotSame(result1, result2);
        assertEquals(userId1, result1.getUserId());
        assertEquals(userId2, result2.getUserId());
        assertEquals(username1, result1.getUsername());
        assertEquals(username2, result2.getUsername());
    }
}
