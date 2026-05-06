package com.tourism.platform.controller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tourism.platform.dto.ConnectionRequestDto;
import com.tourism.platform.dto.SendConnectionRequestDto;
import com.tourism.platform.dto.SendMessageRequest;
import com.tourism.platform.dto.TravelerSummaryDto;
import com.tourism.platform.model.Message;
import com.tourism.platform.model.MessageStatus;
import com.tourism.platform.model.User;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.security.CustomUserDetailsService;
import com.tourism.platform.security.JwtTokenProvider;
import com.tourism.platform.service.SocialService;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SocialControllerTest {

    @Mock
    private SocialService socialService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private User travelerPrincipal(long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@t.com");
        u.setPassword("pw");
        u.setFirstName("F");
        u.setLastName("L");
        u.setRole(UserRole.TRAVELER);
        return u;
    }

    /** Sets {@link SecurityContextHolder} so {@code SocialController#getCurrentUserId} sees our domain {@link User}. */
    private static RequestPostProcessor domainUser(User user) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            return request;
        };
    }

    @BeforeEach
    void setUp() {
        SocialController controller = new SocialController(socialService, jwtTokenProvider, customUserDetailsService);
        ReflectionTestUtils.setField(controller, "demoEndpointsEnabled", true);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
        void sendConnectionRequestCreated() throws Exception {
        ConnectionRequestDto out = new ConnectionRequestDto();
        out.setId(10L);
        out.setStatus("PENDING");
        when(socialService.sendConnectionRequest(any(SendConnectionRequestDto.class), eq(5L))).thenReturn(out);

        SendConnectionRequestDto body = new SendConnectionRequestDto();
        body.setRecipientId(3L);
        body.setMessage("hello");

        mockMvc.perform(post("/social/connections")
                        .with(domainUser(travelerPrincipal(5L, "alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
        void getUserConnectionsOk() throws Exception {
        when(socialService.getUserConnections(2L)).thenReturn(List.of());

        mockMvc.perform(get("/social/connections/{userId}", 2L)
                        .with(domainUser(travelerPrincipal(2L, "u2"))))
                .andExpect(status().isOk());
    }

    @Test
        void acceptConnectionRequestOk() throws Exception {
        ConnectionRequestDto out = new ConnectionRequestDto();
        out.setStatus("ACCEPTED");
        when(socialService.acceptConnectionRequest(9L, 4L)).thenReturn(out);

        mockMvc.perform(put("/social/connections/{id}/accept", 9L)
                        .with(domainUser(travelerPrincipal(4L, "bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
        void rejectConnectionRequestOk() throws Exception {
        ConnectionRequestDto out = new ConnectionRequestDto();
        out.setStatus("REJECTED");
        when(socialService.rejectConnectionRequest(9L, 4L)).thenReturn(out);

        mockMvc.perform(put("/social/connections/{id}/reject", 9L)
                        .with(domainUser(travelerPrincipal(4L, "bob"))))
                .andExpect(status().isOk());
    }

    @Test
        void pendingAndSentRequestsOk() throws Exception {
        when(socialService.getPendingRequestsForUser(1L)).thenReturn(List.of());
        when(socialService.getSentRequestsForUser(1L)).thenReturn(List.of());

        mockMvc.perform(get("/social/connections/pending")
                        .with(domainUser(travelerPrincipal(1L, "u"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/social/connections/sent")
                        .with(domainUser(travelerPrincipal(1L, "u"))))
                .andExpect(status().isOk());
    }

    @Test
        void removeConnectionOk() throws Exception {
        mockMvc.perform(delete("/social/connections/{connectionId}", 7L)
                        .with(domainUser(travelerPrincipal(1L, "u"))))
                .andExpect(status().isOk());

        verify(socialService).deleteConnection(7L, 1L);
    }

    @Test
        void getTravelerSummaryOk() throws Exception {
        TravelerSummaryDto summary = TravelerSummaryDto.builder().userId(3L).displayName("X").build();
        when(socialService.getTravelerSummary(3L)).thenReturn(summary);

        mockMvc.perform(get("/social/travelers/{id}/summary", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(3));
    }

    @Test
        void reviewsFlowOk() throws Exception {
        mockMvc.perform(get("/social/reviews/{type}/{id}", "destination", 1L)
                        .with(domainUser(travelerPrincipal(1L, "u"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/social/reviews")
                        .with(domainUser(travelerPrincipal(1L, "u")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":1,\"targetType\":\"destination\",\"rating\":5,\"comment\":\"nice\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/social/reviews/{id}", 8L)
                        .with(domainUser(travelerPrincipal(1L, "u")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"ok\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/social/reviews/{id}", 8L)
                        .with(domainUser(travelerPrincipal(1L, "u"))))
                .andExpect(status().isOk());
    }

    @Test
        void conversationsAndMessagesOk() throws Exception {
        mockMvc.perform(get("/social/conversations/{userId}", 2L)
                        .with(domainUser(travelerPrincipal(2L, "u2"))))
                .andExpect(status().isOk());

        when(socialService.getConversationMessagesPaginated(1L, 2L, 0, 50))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/social/connections/{connectionId}/messages", 1L)
                        .with(domainUser(travelerPrincipal(2L, "u2")))
                        .param("userId", "2"))
                .andExpect(status().isOk());

        doNothing().when(socialService).markMessageAsRead(99L, 1L);
        mockMvc.perform(put("/social/messages/{messageId}/read", 99L)
                        .with(domainUser(travelerPrincipal(1L, "u"))))
                .andExpect(status().isOk());
    }

    @Test
        void feedAndPostsOk() throws Exception {
        mockMvc.perform(get("/social/feed/{userId}", 1L)
                        .with(domainUser(travelerPrincipal(1L, "u"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/social/posts")
                        .with(domainUser(travelerPrincipal(1L, "u")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hi\",\"type\":\"POST\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/social/posts/{postId}/like", 5L)
                        .with(domainUser(travelerPrincipal(1L, "u"))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/social/posts/{postId}/like", 5L)
                        .with(domainUser(travelerPrincipal(1L, "u"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/social/posts/{postId}/comments", 5L)
                        .with(domainUser(travelerPrincipal(1L, "u")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"c\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/social/posts/{postId}/comments", 5L)
                        .with(domainUser(travelerPrincipal(1L, "u"))))
                .andExpect(status().isOk());
    }

    @Test
        void sendMessageOk() throws Exception {
        Message msg = new Message(1L, 2L, 3L, "hey", MessageStatus.SENT);
        msg.setId(50L);
        when(socialService.sendMessage(1L, 2L, "hey")).thenReturn(msg);

        SendMessageRequest body = new SendMessageRequest(1L, 2L, "hey");

        mockMvc.perform(post("/social/messages")
                        .with(domainUser(travelerPrincipal(2L, "u2")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(50));
    }

    /**
     * Branch 1 (getCurrentUserId): auth != null, isAuthenticated(), principal instanceof User
     * → returns user.getId() directly, no token needed.
         * Already implicitly covered by sendConnectionRequestCreated, but made explicit here.
     */
    @Test
        void getCurrentUserIdWithDomainUserPrincipalReturnsUserId() throws Exception {
        ConnectionRequestDto out = new ConnectionRequestDto();
        out.setStatus("PENDING");
        when(socialService.sendConnectionRequest(any(), eq(99L))).thenReturn(out);

        SendConnectionRequestDto body = new SendConnectionRequestDto();
        body.setRecipientId(3L);

        mockMvc.perform(post("/social/connections")
                        .with(domainUser(travelerPrincipal(99L, "alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        verify(socialService).sendConnectionRequest(any(), eq(99L));
    }

    /**
     * Branch 2 + extractTokenFromRequest Branch 1:
     * No SecurityContext principal → falls through to token.
     * Header starts with "Bearer " → token extracted.
     * tokenProvider.validateToken == true, getUserIdFromJWT returns non-null → returns userId.
     */
    @Test
        void getCurrentUserIdWithValidBearerTokenReturnsUserId() throws Exception {
        SecurityContextHolder.clearContext(); // no domain principal

        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJWT("valid.jwt.token")).thenReturn(7L);

        ConnectionRequestDto out = new ConnectionRequestDto();
        out.setStatus("PENDING");
        when(socialService.sendConnectionRequest(any(), eq(7L))).thenReturn(out);

        SendConnectionRequestDto body = new SendConnectionRequestDto();
        body.setRecipientId(3L);

        mockMvc.perform(post("/social/connections")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        verify(socialService).sendConnectionRequest(any(), eq(7L));
    }

    @Test
        void getCurrentUserIdWithValidTokenButNullUserIdThrows() {
        SecurityContextHolder.clearContext();

        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJWT("valid.jwt.token")).thenReturn(null);

        SendConnectionRequestDto body = new SendConnectionRequestDto();
        body.setRecipientId(3L);

        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/social/connections")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))));

        assertThat(ex.getCause()).isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("User is not authenticated");
    }

    @Test
        void getCurrentUserIdWithNoTokenAndNoPrincipalThrows() {
        SecurityContextHolder.clearContext();

        SendConnectionRequestDto body = new SendConnectionRequestDto();
        body.setRecipientId(3L);

        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/social/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))));

        assertThat(ex.getCause()).isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("User is not authenticated");
    }

    @Test
        void getCurrentUserIdWithNonBearerAuthHeaderThrows() {
        SecurityContextHolder.clearContext();

        SendConnectionRequestDto body = new SendConnectionRequestDto();
        body.setRecipientId(3L);

        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/social/connections")
                        .header("Authorization", "Basic dXNlcjpwYXNz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))));

        assertThat(ex.getCause()).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
        void getCurrentUserIdWithInvalidTokenThrows() {
        SecurityContextHolder.clearContext();

        when(jwtTokenProvider.validateToken("bad.token")).thenReturn(false);

        SendConnectionRequestDto body = new SendConnectionRequestDto();
        body.setRecipientId(3L);

        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/social/connections")
                        .header("Authorization", "Bearer bad.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))));

        assertThat(ex.getCause()).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void getUserConnectionsDeniedForOtherNonAdminUser() {
        assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/social/connections/{userId}", 2L)
                        .with(domainUser(travelerPrincipal(9L, "intruder")))));
    }

    @Test
    void getUserConnectionsAllowedForAdminViewingOtherUser() throws Exception {
        User admin = travelerPrincipal(1L, "admin");
        admin.setRole(UserRole.ADMIN);
        when(socialService.getUserConnections(3L)).thenReturn(List.of());

        mockMvc.perform(get("/social/connections/{userId}", 3L).with(domainUser(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void demoEndpointsReturnErrorWhenDisabled() {
        SocialController disabledController = new SocialController(socialService, jwtTokenProvider, customUserDetailsService);
        ReflectionTestUtils.setField(disabledController, "demoEndpointsEnabled", false);
        MockMvc disabledMvc = MockMvcBuilders.standaloneSetup(disabledController).build();

        assertThrows(ServletException.class, () ->
                disabledMvc.perform(post("/social/reviews")
                        .with(domainUser(travelerPrincipal(1L, "u")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetId\":1,\"targetType\":\"d\",\"rating\":5,\"comment\":\"x\"}")));
    }

    @Test
    void sendMessageRejectsSenderIdImpersonation() {
        SendMessageRequest body = new SendMessageRequest(1L, 999L, "hi");
        assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/social/messages")
                        .with(domainUser(travelerPrincipal(2L, "victim")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))));
    }

    @Test
    void sendMessageAllowsOmittedSenderId() throws Exception {
        Message msg = new Message(1L, 2L, 3L, "hey", MessageStatus.SENT);
        msg.setId(1L);
        when(socialService.sendMessage(1L, 2L, "hey")).thenReturn(msg);
        String json = "{\"connectionId\":1,\"content\":\"hey\"}";

        mockMvc.perform(post("/social/messages")
                        .with(domainUser(travelerPrincipal(2L, "u2")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void getConversationMessagesRejectsMismatchedUserIdQuery() {
        assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/social/connections/{connectionId}/messages", 1L)
                        .with(domainUser(travelerPrincipal(2L, "u2")))
                        .param("userId", "99")));
    }

    @Test
    void getCurrentUserIdResolvesFromSpringSecurityUserDetailsPrincipal() throws Exception {
        UserDetails springUser = org.springframework.security.core.userdetails.User.builder()
                .username("legacy")
                .password("x")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_TRAVELER")))
                .build();
        User domain = travelerPrincipal(11L, "legacy");
        when(customUserDetailsService.loadUserByUsername("legacy")).thenReturn(domain);

        ConnectionRequestDto out = new ConnectionRequestDto();
        when(socialService.sendConnectionRequest(any(), eq(11L))).thenReturn(out);

        SendConnectionRequestDto dto = new SendConnectionRequestDto();
        dto.setRecipientId(7L);
        mockMvc.perform(post("/social/connections")
                        .with(req -> {
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(springUser, null, springUser.getAuthorities()));
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        verify(socialService).sendConnectionRequest(any(), eq(11L));
    }
}
