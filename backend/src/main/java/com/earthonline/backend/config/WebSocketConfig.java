package com.earthonline.backend.config;

import com.earthonline.backend.auth.WebSocketJwtChannelInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * WebSocket/STOMP 的基础入口。
 *
 * 本阶段只建立连接通道：
 * - /ws/chat 是浏览器建立 WebSocket 连接的地址；
 * - /app 是客户端以后发送聊天指令的前缀；
 * - /user 是服务器以后向某一位用户推送消息的前缀；
 * - /queue、/topic 是内存代理实际处理的目的地前缀（私人队列翻译后落在 /queue）。
 *
 * 现有 REST 聊天接口不会受到影响，仍用于加载历史消息和作为兜底。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final WebSocketJwtChannelInterceptor webSocketJwtChannelInterceptor;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(
            WebSocketJwtChannelInterceptor webSocketJwtChannelInterceptor,
            @Value("${chat.websocket.allowed-origin-patterns:http://localhost:5173,http://127.0.0.1:5173}") String allowedOriginPatterns
    ) {
        this.webSocketJwtChannelInterceptor = webSocketJwtChannelInterceptor;
        this.allowedOriginPatterns = resolveAllowedOriginPatterns(allowedOriginPatterns);
        // 部署后可直接在日志里确认白名单，Origin 不匹配时握手会返回 403。
        log.info("WebSocket /ws/chat 允许的浏览器 Origin: {}", String.join(", ", this.allowedOriginPatterns));
    }

    /**
     * 解析 Origin 白名单，并做两件防御：
     * 1. 丢弃空值（例如环境变量存在但是空字符串，会让所有来源都被拒绝，
     *    表现为“页面能打开、历史消息正常、实时聊天和未读一直不更新”）；
     * 2. 结果为空时回退到本地开发地址，避免误配置直接把聊天锁死。
     */
    private static String[] resolveAllowedOriginPatterns(String rawPatterns) {
        String[] parsed = rawPatterns == null
                ? new String[0]
                : Arrays.stream(rawPatterns.split(","))
                        .map(String::trim)
                        .filter(pattern -> !pattern.isEmpty())
                        .toArray(String[]::new);

        if (parsed.length > 0) {
            return parsed;
        }
        return new String[]{"http://localhost:5173", "http://127.0.0.1:5173"};
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 简单代理只处理带这些前缀的目的地（Spring 内部用 checkDestinationPrefix 过滤）。
        // 私人消息必须保留 /queue：浏览器订阅 /user/queue/... 以及
        // SimpMessagingTemplate 向 /user/{username}/queue/... 推送时，
        // Spring 都会先把目的地翻译成 /queue/xxx-user{sessionId} 再交给代理。
        // 若这里只写 /user，翻译后的目的地不匹配任何前缀，消息会被静默丢弃。
        registry.enableSimpleBroker("/queue", "/topic");
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
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}
