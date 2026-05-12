package com.tourism.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${app.websocket.allowed-origin-patterns:http://localhost:5173}")
    private String allowedOriginPatterns;
    /**
     * Configure the message broker used for in-memory STOMP messaging.
     *
     * @param config registry used to configure application destination prefixes and brokers
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register STOMP endpoints to which WebSocket clients can connect.
     *
     * @param registry registry used to add STOMP endpoints and configure SockJS fallback
     */
    @Override
    @SuppressWarnings("null")
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        String[] originPatterns = java.util.Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns(originPatterns)
                .withSockJS();
    }
}