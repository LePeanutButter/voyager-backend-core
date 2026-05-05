package com.tourism.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelPlanBusinessMethodsTest {

    @Test
    void addAndRemoveActivity_maintainsBidirectionalAssociation() {
        TravelPlan plan = new TravelPlan();
        TravelPlanActivity act = new TravelPlanActivity();
        act.setName("Act");
        act.setStartTime(LocalDateTime.now());
        act.setEndTime(LocalDateTime.now().plusHours(1));

        plan.addActivity(act);
        assertTrue(plan.getActivities().contains(act));
        assertEquals(plan, act.getTravelPlan());

        plan.removeActivity(act);
        assertFalse(plan.getActivities().contains(act));
    }

    @Test
    void addAndRemoveReservation() {
        TravelPlan plan = new TravelPlan();
        User u = User.builder().id(1L).username("a").email("a@a.com").password("p").firstName("f").lastName("l").build();
        Reservation r = new Reservation(u, "R", ReservationType.HOTEL);
        r.setStartDate(LocalDateTime.now());
        r.setEndDate(LocalDateTime.now().plusDays(1));

        plan.addReservation(r);
        assertTrue(plan.getReservations().contains(r));
        assertEquals(plan, r.getTravelPlan());

        plan.removeReservation(r);
        assertFalse(plan.getReservations().contains(r));
    }

    @Test
    void isCompleted_and_isActive() {
        TravelPlan p = new TravelPlan();
        p.setStatus(TravelPlanStatus.COMPLETED);
        assertTrue(p.isCompleted());
        p.setStatus(TravelPlanStatus.ACTIVE);
        assertTrue(p.isActive());
    }

    @Test
    void getDurationInDays() {
        TravelPlan p = new TravelPlan();
        LocalDateTime s = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime e = LocalDateTime.of(2026, 1, 5, 0, 0);
        p.setStartDate(s);
        p.setEndDate(e);
        assertEquals(4, p.getDurationInDays());
    }

    @Test
    void getDurationInDays_whenDatesMissing() {
        TravelPlan p = new TravelPlan();
        p.setStartDate(null);
        p.setEndDate(LocalDateTime.now());
        assertEquals(0, p.getDurationInDays());
    }

    @Test
    void threeArgConstructor() {
        User u = User.builder().id(1L).username("a").email("a@a.com").password("p").firstName("f").lastName("l").build();
        TravelPlan p = new TravelPlan(u, "Paris trip", "Paris");
        assertEquals(u, p.getUser());
        assertEquals("Paris trip", p.getTitle());
        assertEquals("Paris", p.getDestinationLocation());
    }

    @Test
    void gettersSetters_coverRemainingFields() {
        TravelPlan p = new TravelPlan();
        User u = User.builder().id(1L).username("a").email("a@a.com").password("p").firstName("f").lastName("l").build();
        LocalDateTime t = LocalDateTime.of(2026, 7, 1, 0, 0);
        p.setId(10L);
        p.setUser(u);
        p.setTitle("T");
        p.setDescription("D");
        p.setStatus(TravelPlanStatus.DRAFT);
        p.setTravelType(TravelType.ADVENTURE);
        p.setStartDate(t);
        p.setEndDate(t.plusDays(3));
        p.setEstimatedBudget(null);
        p.setActualCost(null);
        p.setNumberOfTravelers(2);
        p.setOriginLocation("NYC");
        p.setDestinationLocation("Tokyo");
        p.setIsPublic(true);
        p.setShareToken("token");
        p.setCreatedAt(t);
        p.setUpdatedAt(t.plusHours(1));

        assertEquals(10L, p.getId());
        assertEquals("T", p.getTitle());
        assertEquals(TravelType.ADVENTURE, p.getTravelType());
        assertEquals(2, p.getNumberOfTravelers());
        assertEquals("NYC", p.getOriginLocation());
        assertTrue(p.getIsPublic());
        assertEquals("token", p.getShareToken());
    }
}
