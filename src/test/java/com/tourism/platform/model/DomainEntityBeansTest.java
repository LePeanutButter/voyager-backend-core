package com.tourism.platform.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainEntityBeansTest {

    @Test
    void sharedSpaceAccess_ConstructorAndAccessors() {
        SharedSpaceAccess a = new SharedSpaceAccess(2L, 3L, "CHAT");
        a.setId(1L);
        assertEquals(2L, a.getConnectionId());
        assertEquals(3L, a.getUserId());
        assertEquals("CHAT", a.getSpaceType());
        assertNotNull(a.getAccessGrantedAt());
        a.setAccessGrantedAt(null);
        assertNull(a.getAccessGrantedAt());
    }

    @Test
    void travelPlanParticipant_Associations() {
        TravelPlan plan = new TravelPlan();
        User user = new User();
        TravelPlanParticipant p = new TravelPlanParticipant();
        p.setTravelPlan(plan);
        p.setUser(user);
        assertSame(plan, p.getTravelPlan());
        assertSame(user, p.getUser());
        assertNull(p.getId());
    }

    @Test
    void userConnection_StatusAndUsers() {
        UserConnection uc = new UserConnection();
        User r = new User();
        User v = new User();
        uc.setRequester(r);
        uc.setRecipient(v);
        uc.setStatus(ConnectionStatus.ACCEPTED);
        assertSame(r, uc.getRequester());
        assertSame(v, uc.getRecipient());
        assertEquals(ConnectionStatus.ACCEPTED, uc.getStatus());
    }

    @Test
    void userInterest_Fields() {
        UserInterest ui = new UserInterest();
        User u = new User();
        ui.setUser(u);
        ui.setInterest("hiking");
        assertEquals("hiking", ui.getInterest());
        assertSame(u, ui.getUser());
    }

    @Test
    void userConnectionStatus_Values() {
        assertEquals(UserConnectionStatus.PENDING, UserConnectionStatus.valueOf("PENDING"));
        assertEquals(3, UserConnectionStatus.values().length);
    }
}
