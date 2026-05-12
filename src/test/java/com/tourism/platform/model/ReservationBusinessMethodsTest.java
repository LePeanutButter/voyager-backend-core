package com.tourism.platform.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationBusinessMethodsTest {

    @Test
    void isActive_whenNowBetweenStartAndEnd() {
        LocalDateTime now = LocalDateTime.now();
        Reservation r = baseReservation();
        r.setStartDate(now.minusHours(1));
        r.setEndDate(now.plusHours(1));
        assertTrue(r.isActive());
    }

    @Test
    void isActive_whenOutsideWindow() {
        LocalDateTime now = LocalDateTime.now();
        Reservation r = baseReservation();
        r.setStartDate(now.plusDays(1));
        r.setEndDate(now.plusDays(2));
        assertFalse(r.isActive());
    }

    @Test
    void isUpcoming_whenStartInFuture() {
        Reservation r = baseReservation();
        r.setStartDate(LocalDateTime.now().plusDays(1));
        assertTrue(r.isUpcoming());
    }

    @Test
    void isCompleted_whenEndInPast() {
        Reservation r = baseReservation();
        r.setEndDate(LocalDateTime.now().minusDays(1));
        assertTrue(r.isCompleted());
    }

    @Test
    void statusFlags() {
        Reservation r = baseReservation();
        r.setStatus(ReservationStatus.CANCELLED);
        assertTrue(r.isCancelled());
        r.setStatus(ReservationStatus.CONFIRMED);
        assertTrue(r.isConfirmed());
    }

    @Test
    void getDurationInDays_withBothDates() {
        Reservation r = baseReservation();
        LocalDateTime s = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime e = LocalDateTime.of(2026, 1, 4, 0, 0);
        r.setStartDate(s);
        r.setEndDate(e);
        assertEquals(3, r.getDurationInDays());
    }

    @Test
    void getDurationInDays_whenStartNull() {
        Reservation r = baseReservation();
        r.setStartDate(null);
        r.setEndDate(LocalDateTime.now());
        assertEquals(0, r.getDurationInDays());
    }

    @Test
    void getRemainingBalance_bothCosts() {
        Reservation r = baseReservation();
        r.setTotalCost(new BigDecimal("100"));
        r.setDepositPaid(new BigDecimal("30"));
        assertEquals(0, new BigDecimal("70").compareTo(r.getRemainingBalance()));
    }

    @Test
    void getRemainingBalance_onlyTotalCost() {
        Reservation r = baseReservation();
        r.setTotalCost(new BigDecimal("50"));
        r.setDepositPaid(null);
        assertEquals(0, new BigDecimal("50").compareTo(r.getRemainingBalance()));
    }

    @Test
    void getRemainingBalance_defaultsToZero() {
        Reservation r = baseReservation();
        r.setTotalCost(null);
        r.setDepositPaid(null);
        assertEquals(BigDecimal.ZERO, r.getRemainingBalance());
    }

    @Test
    void accessors_roundTrip() {
        Reservation r = new Reservation();
        User u = User.builder().id(1L).username("a").email("a@a.com").password("p").firstName("f").lastName("l").build();
        TravelPlan p = new TravelPlan();
        p.setId(9L);
        r.setId(1L);
        r.setUser(u);
        r.setTravelPlan(p);
        r.setName("n");
        r.setDescription("d");
        r.setType(ReservationType.FLIGHT);
        r.setStatus(ReservationStatus.PENDING);
        r.setConfirmationNumber("c");
        LocalDateTime t = LocalDateTime.of(2026, 6, 1, 12, 0);
        r.setStartDate(t);
        r.setEndDate(t.plusDays(1));
        r.setLocation("loc");
        r.setServiceProvider("sp");
        r.setTotalCost(BigDecimal.ONE);
        r.setDepositPaid(BigDecimal.ZERO);
        r.setIsPaid(true);
        r.setPaymentMethod("card");
        r.setCancellationPolicy("none");
        r.setSpecialRequests("window");
        r.setContactInfo("phone");
        r.setCreatedAt(t);
        r.setUpdatedAt(t);

        assertEquals(1L, r.getId());
        assertEquals(u, r.getUser());
        assertEquals(p, r.getTravelPlan());
        assertEquals("n", r.getName());
        assertEquals("d", r.getDescription());
        assertEquals(ReservationType.FLIGHT, r.getType());
        assertEquals(ReservationStatus.PENDING, r.getStatus());
        assertEquals("c", r.getConfirmationNumber());
        assertEquals(t, r.getStartDate());
        assertEquals(t.plusDays(1), r.getEndDate());
        assertEquals("loc", r.getLocation());
        assertEquals("sp", r.getServiceProvider());
        assertTrue(r.getIsPaid());
        assertEquals("card", r.getPaymentMethod());
        assertEquals("none", r.getCancellationPolicy());
        assertEquals("window", r.getSpecialRequests());
        assertEquals("phone", r.getContactInfo());
        assertEquals(t, r.getCreatedAt());
        assertEquals(t, r.getUpdatedAt());
    }

    @Test
    void constructor_threeArgs() {
        User u = User.builder().id(1L).username("a").email("a@a.com").password("p").firstName("f").lastName("l").build();
        Reservation r = new Reservation(u, "Hotel", ReservationType.HOTEL);
        assertEquals(u, r.getUser());
        assertEquals("Hotel", r.getName());
        assertEquals(ReservationType.HOTEL, r.getType());
    }

    private static Reservation baseReservation() {
        Reservation r = new Reservation();
        User u = User.builder().id(1L).username("a").email("a@a.com").password("p").firstName("f").lastName("l").build();
        r.setUser(u);
        r.setName("x");
        r.setStartDate(LocalDateTime.now());
        r.setEndDate(LocalDateTime.now().plusHours(2));
        return r;
    }
}
