package edu.mines.packtrain.config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("https://localhost.dev", "https://packtrain.mines.edu")
            .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

//    @Override
//    public void configureClientInboundChannel(ChannelRegistration registration) {
//        registration.interceptors(new ChannelInterceptor() {
//            @Override
//            public Message<?> preSend(Message<?> message, MessageChannel channel) {
//                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
//
//                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
//                    String authHeader = accessor.getFirstNativeHeader("Authorization");
//                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
//                        String token = authHeader.substring(7);
//                        try {
//                            Jwt jwt = jwtDecoder.decode(token);
//
//                            // Example: set user with ROLE_USER
//                            UsernamePasswordAuthenticationToken user =
//                                    new UsernamePasswordAuthenticationToken(
//                                            jwt.getSubject(),
//                                            null,
//                                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
//                                    );
//
//                            accessor.setUser(user);
//                            SecurityContextHolder.getContext().setAuthentication(user);
//                        } catch (JwtException e) {
//                            throw new IllegalArgumentException("Invalid JWT token", e);
//                        }
//                    } else {
//                        throw new IllegalArgumentException("Missing Authorization header in CONNECT frame");
//                    }
//                }
//
//                return message;
//            }
//        });
//    }
}