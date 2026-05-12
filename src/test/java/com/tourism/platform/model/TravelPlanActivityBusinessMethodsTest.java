package com.tourism.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelPlanActivityBusinessMethodsTest {

    @Test
    void isCompleted_whenEndBeforeNow() {
        TravelPlanActivity a = activity();
        a.setEndTime(LocalDateTime.now().minusMinutes(5));
        assertTrue(a.isCompleted());
    }

    @Test
    void isUpcoming_whenStartAfterNow() {
        TravelPlanActivity a = activity();
        a.setStartTime(LocalDateTime.now().plusDays(1));
        a.setEndTime(LocalDateTime.now().plusDays(2));
        assertTrue(a.isUpcoming());
    }

    @Test
    void isInProgress_whenNowBetweenStartAndEnd() {
        LocalDateTime now = LocalDateTime.now();
        TravelPlanActivity a = activity();
        a.setStartTime(now.minusMinutes(10));
        a.setEndTime(now.plusMinutes(10));
        assertTrue(a.isInProgress());
    }

    @Test
    void getDurationInMinutes_whenBothSet() {
        TravelPlanActivity a = activity();
        LocalDateTime s = LocalDateTime.of(2026, 3, 1, 10, 0);
        LocalDateTime e = LocalDateTime.of(2026, 3, 1, 10, 45);
        a.setStartTime(s);
        a.setEndTime(e);
        assertEquals(45, a.getDurationInMinutes());
    }

    @Test
    void getDurationInMinutes_whenStartNull() {
        TravelPlanActivity a = activity();
        a.setStartTime(null);
        a.setEndTime(LocalDateTime.now());
        assertEquals(0, a.getDurationInMinutes());
    }

    @Test
    void accessors_roundTrip() {
        TravelPlan plan = new TravelPlan();
        plan.setId(1L);
        TravelPlanActivity a = new TravelPlanActivity();
        LocalDateTime t = LocalDateTime.of(2026, 4, 1, 8, 0);
        a.setId(2L);
        a.setTravelPlan(plan);
        a.setName("Tour");
        a.setDescription("desc");
        a.setType(ActivityType.DINING);
        a.setStartTime(t);
        a.setEndTime(t.plusHours(2));
        a.setLocation("Rome");
        a.setEstimatedCost(null);
        a.setActualCost(null);
        a.setBookingReference("BR");
        a.setIsConfirmed(true);
        a.setNotes("n");
        a.setCreatedAt(t);
        a.setUpdatedAt(t.plusHours(1));

        assertEquals(2L, a.getId());
        assertEquals(plan, a.getTravelPlan());
        assertEquals("Tour", a.getName());
        assertEquals(ActivityType.DINING, a.getType());
        assertEquals("Rome", a.getLocation());
        assertTrue(a.getIsConfirmed());
        assertEquals("n", a.getNotes());
    }

    @Test
    void constructor_withPlanAndTimes() {
        TravelPlan plan = new TravelPlan();
        LocalDateTime s = LocalDateTime.now();
        LocalDateTime e = s.plusHours(1);
        TravelPlanActivity a = new TravelPlanActivity(plan, "Name", s, e);
        assertEquals(plan, a.getTravelPlan());
        assertEquals("Name", a.getName());
        assertEquals(s, a.getStartTime());
        assertEquals(e, a.getEndTime());
    }

    private static TravelPlanActivity activity() {
        TravelPlanActivity a = new TravelPlanActivity();
        a.setTravelPlan(new TravelPlan());
        a.setName("n");
        a.setStartTime(LocalDateTime.now().minusHours(1));
        a.setEndTime(LocalDateTime.now().plusHours(1));
        return a;
    }
}
