package com.earthonline.backend.config;

import com.earthonline.backend.auth.WebSocketJwtChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket/STOMP 的基础入口。
 *
 * 本阶段只建立连接通道：
 * - /ws/chat 是浏览器建立 WebSocket 连接的地址；
 * - /app 是客户端以后发送聊天指令的前缀；
 * - /user 是服务器以后向某一位用户推送消息的前缀。
 *
 * 现有 REST 聊天接口不会受到影响，仍用于加载历史消息和作为兜底。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketJwtChannelInterceptor webSocketJwtChannelInterceptor;

    public WebSocketConfig(
            WebSocketJwtChannelInterceptor webSocketJwtChannelInterceptor
    ) {
        this.webSocketJwtChannelInterceptor = webSocketJwtChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketJwtChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("http://localhost:5173");
    }
}
