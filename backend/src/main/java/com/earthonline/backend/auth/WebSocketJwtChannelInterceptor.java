package com.earthonline.backend.auth;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
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
        // 必须取出消息原本的 accessor，不能直接用 StompHeaderAccessor.wrap(message)：
        // wrap(...) 返回的是 new StompHeaderAccessor(message) 副本，
        // 在副本上 setUser 既不会写进消息头，也不会触发 StompSubProtocolHandler
        // 注册的 userChangeCallback，于是 WebSocket 会话没有 Principal、
        // SimpUserRegistry 里也没有这个用户，
        // 之后 SimpMessagingTemplate.convertAndSendToUser(username, ...) 找不到会话，
        // 新消息、已读回执、好友事件等私人队列推送都会被静默丢弃。
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }

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
