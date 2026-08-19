package com.earthonline.backend.auth;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates the JWT supplied in a STOMP CONNECT frame and binds its username
 * to the WebSocket session. The WebSocket HTTP handshake itself is public;
 * actual access is granted only after this interceptor accepts CONNECT.
 */
@Component
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketJwtChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");

            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new AccessDeniedException("WebSocket connection requires a JWT");
            }

            try {
                String token = authorization.substring(7).trim();
                String username = jwtService.extractUsername(token);

                accessor.setUser(new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of()
                ));
            } catch (Exception exception) {
                throw new AccessDeniedException("Invalid WebSocket JWT", exception);
            }
        }

        return message;
    }
}
