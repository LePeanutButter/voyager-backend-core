package com.tourism.platform.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAuthoritiesTest {

    @Test
    void getAuthorities_reflectsRole() {
        User u = User.builder()
                .username("admin")
                .email("a@a.com")
                .password("p")
                .firstName("A")
                .lastName("B")
                .role(UserRole.ADMIN)
                .build();

        assertTrue(u.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
    }
}
