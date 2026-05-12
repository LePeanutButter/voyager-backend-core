package com.tourism.platform.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.servlet.FilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenNoAuthorizationHeader_doesNotAuthenticate() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(tokenProvider, never()).validateToken(anyString());
        verify(chain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void whenBearerInvalid_doesNotAuthenticate() throws Exception {
        when(tokenProvider.validateToken("tok")).thenReturn(false);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    void whenValidToken_setsAuthentication() throws Exception {
        when(tokenProvider.validateToken("good")).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT("good")).thenReturn("alice");
        UserDetails ud = User.withUsername("alice").password("p").roles("USER").build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(ud);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer good");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void whenUserDetailsThrows_swallowsAndContinuesChain() throws Exception {
        when(tokenProvider.validateToken("x")).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT("x")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("nope"));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer x");
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void whenJwtParsingThrows_swallowsAndContinuesChain() throws Exception {
        when(tokenProvider.validateToken("x")).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT("x")).thenThrow(new JwtException("bad"));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer x");
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
