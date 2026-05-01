package com.tourism.platform.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

class WebSocketConfigTest {

    @Test
    void configuresBrokerAndRegistersEndpoint() {
        WebSocketConfig cfg = new WebSocketConfig();

        MessageBrokerRegistry broker = Mockito.mock(MessageBrokerRegistry.class);
        cfg.configureMessageBroker(broker);
        Mockito.verify(broker).enableSimpleBroker("/topic", "/queue");
        Mockito.verify(broker).setApplicationDestinationPrefixes("/app");

        StompEndpointRegistry stomp = Mockito.mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration reg = Mockito.mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration sockJs = Mockito.mock(SockJsServiceRegistration.class);
        Mockito.when(stomp.addEndpoint("/ws-chat")).thenReturn(reg);
        Mockito.when(reg.setAllowedOriginPatterns(Mockito.any())).thenReturn(reg);
        Mockito.when(reg.withSockJS()).thenReturn(sockJs);

        cfg.registerStompEndpoints(stomp);

        Mockito.verify(stomp).addEndpoint("/ws-chat");
        Mockito.verify(reg).setAllowedOriginPatterns("*");
        Mockito.verify(reg).withSockJS();
    }
}
