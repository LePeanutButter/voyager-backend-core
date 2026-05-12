package com.tourism.platform.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

class WebSocketConfigTest {

    @Test
    @SuppressWarnings("null")
    void configuresBrokerAndRegistersEndpoint() {
        WebSocketConfig cfg = new WebSocketConfig();
        ReflectionTestUtils.setField(cfg, "allowedOriginPatterns", "http://localhost:5173,https://app.example.com");

        MessageBrokerRegistry broker = Mockito.mock(MessageBrokerRegistry.class);
        cfg.configureMessageBroker(broker);
        Mockito.verify(broker).enableSimpleBroker("/topic", "/queue");
        Mockito.verify(broker).setApplicationDestinationPrefixes("/app");

        StompEndpointRegistry stomp = Mockito.mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration reg = Mockito.mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration sockJs = Mockito.mock(SockJsServiceRegistration.class);
        Mockito.when(stomp.addEndpoint("/ws-chat")).thenReturn(reg);
        Mockito.when(reg.setAllowedOriginPatterns(Mockito.any(String[].class))).thenReturn(reg);
        Mockito.when(reg.withSockJS()).thenReturn(sockJs);

        cfg.registerStompEndpoints(stomp);

        Mockito.verify(stomp).addEndpoint("/ws-chat");
        Mockito.verify(reg).setAllowedOriginPatterns("http://localhost:5173", "https://app.example.com");
        Mockito.verify(reg).withSockJS();
    }

    @Test
    @SuppressWarnings("null")
    void registerStompEndpointsTrimsEmptySegmentsAndSpaces() {
        WebSocketConfig cfg = new WebSocketConfig();
        ReflectionTestUtils.setField(cfg, "allowedOriginPatterns", " http://a.test , ,https://b.test ");

        StompEndpointRegistry stomp = Mockito.mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration reg = Mockito.mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration sockJs = Mockito.mock(SockJsServiceRegistration.class);
        Mockito.when(stomp.addEndpoint("/ws-chat")).thenReturn(reg);
        Mockito.when(reg.setAllowedOriginPatterns(Mockito.any(String[].class))).thenReturn(reg);
        Mockito.when(reg.withSockJS()).thenReturn(sockJs);

        cfg.registerStompEndpoints(stomp);

        Mockito.verify(reg).setAllowedOriginPatterns("http://a.test", "https://b.test");
    }
}
