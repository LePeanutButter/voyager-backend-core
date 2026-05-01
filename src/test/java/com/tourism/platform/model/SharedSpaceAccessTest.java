package com.tourism.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.time.temporal.ChronoUnit;

class SharedSpaceAccessTest {

    // ── Constructors ─────────────────────────────────────────────────────────

    @Test
    void noArgsConstructor_ShouldCreateInstanceWithNullFields() {
        SharedSpaceAccess access = new SharedSpaceAccess();

        assertThat(access.getId()).isNull();
        assertThat(access.getConnectionId()).isNull();
        assertThat(access.getUserId()).isNull();
        assertThat(access.getSpaceType()).isNull();
        assertThat(access.getAccessGrantedAt()).isNull();
    }

    @Test
    void allArgsConstructor_ShouldSetFieldsCorrectly() {
        LocalDateTime before = LocalDateTime.now();

        SharedSpaceAccess access = new SharedSpaceAccess(10L, 20L, "ITINERARY");

        LocalDateTime after = LocalDateTime.now();

        assertThat(access.getConnectionId()).isEqualTo(10L);
        assertThat(access.getUserId()).isEqualTo(20L);
        assertThat(access.getSpaceType()).isEqualTo("ITINERARY");
        assertThat(access.getId()).isNull(); // not set by constructor, handled by JPA
        assertThat(access.getAccessGrantedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    void allArgsConstructor_ShouldSetAccessGrantedAtToNow() {
        LocalDateTime before = LocalDateTime.now();

        SharedSpaceAccess access = new SharedSpaceAccess(1L, 2L, "BUDGET");

        assertThat(access.getAccessGrantedAt())
                .isCloseTo(before, within(1, ChronoUnit.SECONDS));
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    @Test
    void setId_ShouldUpdateId() {
        SharedSpaceAccess access = new SharedSpaceAccess();
        access.setId(99L);

        assertThat(access.getId()).isEqualTo(99L);
    }

    @Test
    void setConnectionId_ShouldUpdateConnectionId() {
        SharedSpaceAccess access = new SharedSpaceAccess();
        access.setConnectionId(42L);

        assertThat(access.getConnectionId()).isEqualTo(42L);
    }

    @Test
    void setUserId_ShouldUpdateUserId() {
        SharedSpaceAccess access = new SharedSpaceAccess();
        access.setUserId(77L);

        assertThat(access.getUserId()).isEqualTo(77L);
    }

    @Test
    void setSpaceType_ShouldUpdateSpaceType() {
        SharedSpaceAccess access = new SharedSpaceAccess();
        access.setSpaceType("PACKING_LIST");

        assertThat(access.getSpaceType()).isEqualTo("PACKING_LIST");
    }

    @Test
    void setAccessGrantedAt_ShouldUpdateAccessGrantedAt() {
        SharedSpaceAccess access = new SharedSpaceAccess();
        LocalDateTime timestamp = LocalDateTime.of(2024, 6, 15, 10, 30, 0);

        access.setAccessGrantedAt(timestamp);

        assertThat(access.getAccessGrantedAt()).isEqualTo(timestamp);
    }

    @Test
    void setAccessGrantedAt_ShouldAllowNull() {
        SharedSpaceAccess access = new SharedSpaceAccess(1L, 2L, "ITINERARY");

        access.setAccessGrantedAt(null);

        assertThat(access.getAccessGrantedAt()).isNull();
    }

    // ── Overwrite after parameterized constructor ─────────────────────────────

    @Test
    void setters_ShouldOverwriteValuesSetByConstructor() {
        SharedSpaceAccess access = new SharedSpaceAccess(10L, 20L, "ITINERARY");

        access.setConnectionId(99L);
        access.setUserId(88L);
        access.setSpaceType("BUDGET");

        assertThat(access.getConnectionId()).isEqualTo(99L);
        assertThat(access.getUserId()).isEqualTo(88L);
        assertThat(access.getSpaceType()).isEqualTo("BUDGET");
    }
}