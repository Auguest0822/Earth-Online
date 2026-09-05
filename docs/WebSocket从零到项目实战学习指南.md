# WebSocket Java 零基础与 Earth Online 答辩指南

> 目标：即使没有 WebSocket 基础，也能看懂本项目的 Java 后端代码，并在答辩时准确说明连接、鉴权、消息落库和实时推送过程。

## 1. 先纠正一个容易混淆的问题

WebSocket 是通信协议，不属于 JavaScript，也不属于 Java。本项目两端使用不同语言：

| 部分 | 技术 | 职责 |
|---|---|---|
| 浏览器客户端 | Vue 3、JavaScript、`@stomp/stompjs` | 建立连接、携带 JWT、订阅消息、更新页面 |
| 服务端 | Java、Spring Boot、Spring WebSocket | 验证身份、处理消息、操作数据库、向指定用户推送 |
| 数据库 | MySQL、JPA | 永久保存聊天记录和已读时间 |
| 网关 | Nginx | 转发 HTTP 请求和 WebSocket Upgrade |

本文以 Java 后端为主。涉及浏览器连接时仍会出现少量 JavaScript，因为浏览器不能直接执行 Java。答辩时可以这样说：

> 项目的 WebSocket 服务端采用 Java 和 Spring Boot 实现；Vue 前端通过 STOMP JavaScript 客户端连接 Java 服务端。JavaScript 只是浏览器客户端，核心鉴权、业务校验、数据库持久化和消息推送都在 Java 后端完成。

## 2. 阅读项目前需要的 Java 基础

### 2.1 类、对象和方法

```java
public class ChatService {
    public void sendMessage() {
        // 方法体
    }
}
```

- `class`：一类对象的设计图。
- `ChatService`：类名。
- `sendMessage()`：方法，表示对象能执行的操作。
- `public`：其他类可以调用。
- `void`：方法没有返回值。

### 2.2 构造器与依赖注入

```java
private final ChatService chatService;

public ChatWebSocketController(ChatService chatService) {
    this.chatService = chatService;
}
```

这叫构造器注入。Spring 创建控制器时，把已经创建好的 `ChatService` 传进来。这样依赖明确，也方便测试。

### 2.3 常用注解

| 注解 | 本项目含义 |
|---|---|
| `@Configuration` | 配置类 |
| `@Controller` | 消息控制器 |
| `@Service` | 业务服务 |
| `@Entity` | 数据库实体 |
| `@MessageMapping` | 接收某个 STOMP 目的地的消息 |
| `@Transactional` | 数据库事务 |
| `@Valid` | 执行参数校验 |

### 2.4 接口与实现

```java
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {
}
```

`implements` 表示实现接口。项目通过重写接口方法，告诉 Spring 使用哪些端点、前缀和拦截器。

### 2.5 record DTO

```java
public record SendChatMessageRequest(
        String recipientPublicId,
        String content,
        String clientMessageId,
        ChatMessageType messageType,
        String imageObjectKey
) {
}
```

`record` 适合只承载数据的对象。这里描述前端发送消息时允许提交哪些字段。DTO 不等于数据库实体。

## 3. HTTP、WebSocket 和 STOMP

### 3.1 HTTP

HTTP 通常是客户端发请求，服务器返回响应，适合登录、历史查询、文件上传和 CRUD。

### 3.2 WebSocket

WebSocket 建立长连接后，客户端和服务器都能主动发送数据：

```text
浏览器 A ───────── 长连接 ───────── Java 服务端
浏览器 B ───────── 长连接 ───────── Java 服务端
```

服务器可以立即把 A 的新消息推送给在线的 B，不需要 B 持续轮询。

### 3.3 STOMP

原生 WebSocket 没有规定业务消息发往哪里。STOMP 在 WebSocket 上增加目的地和订阅规则：

| 地址 | 含义 |
|---|---|
| `/ws/chat` | 建立 WebSocket 连接的端点 |
| `/app/chat/messages` | 客户端向 Java 控制器发送消息 |
| `/user/queue/chat/messages` | 用户订阅自己的聊天消息 |
| `/user/queue/chat/read-receipts` | 用户订阅已读回执 |
| `/user/queue/friends/events` | 用户订阅好友事件 |

一句话记忆：WebSocket 是运输道路，STOMP 是道路上的地址和交通规则。

## 4. 项目的真实聊天架构

当前项目采用 REST 与 WebSocket 混合方案：

```text
发送者 Vue 页面
   │ POST /api/chat/messages
   ▼
ChatController（Java）
   ▼
ChatService（校验好友关系、去重、保存 MySQL）
   ├── HTTP 响应返回发送者
   └── WebSocket/STOMP 推送给接收者
          /user/queue/chat/messages
```

历史消息使用 `GET /api/chat/messages?friendPublicId=好友ID` 查询。

项目还保留 Java 的纯 STOMP 入口 `/app/chat/messages`，但当前 `FriendsView.vue` 的主要发送链路使用 REST。答辩时应准确表达：

> REST 负责请求、落库和返回结果，WebSocket 负责把新消息等事件实时推送给在线用户；重新连接后再通过 REST 补齐数据库中的权威数据。

## 5. Java 开启 WebSocket/STOMP

项目文件：`backend/src/main/java/com/earthonline/backend/config/WebSocketConfig.java`

### 5.1 开启消息代理

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {
}
```

- `@Configuration`：交给 Spring 管理的配置类。
- `@EnableWebSocketMessageBroker`：开启 WebSocket/STOMP。
- `WebSocketMessageBrokerConfigurer`：允许项目自定义消息代理。

### 5.2 配置目的地前缀

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/user");
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
}
```

逐句理解：

1. `enableSimpleBroker("/user")`：开启 Spring 内存消息代理。
2. `setApplicationDestinationPrefixes("/app")`：发往 `/app/**` 的消息交给 Java 的 `@MessageMapping`。
3. `setUserDestinationPrefix("/user")`：私人消息使用 `/user` 前缀。

### 5.3 注册握手端点

```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/chat")
            .setAllowedOriginPatterns("http://localhost:5173");
}
```

浏览器先连接 `/ws/chat`。它只是握手入口，不是聊天消息发送地址。

注意：当前代码只允许本地开发地址。正式部署时应加入实际前端来源，最好从环境变量读取，不能长期写成任意来源 `*`。

### 5.4 注册 JWT 拦截器

```java
@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(webSocketJwtChannelInterceptor);
}
```

从浏览器进入服务器的 STOMP 帧先经过拦截器，在到达控制器前完成身份验证。

## 6. Java 验证 WebSocket JWT

项目文件：`backend/src/main/java/com/earthonline/backend/auth/WebSocketJwtChannelInterceptor.java`

REST 每次请求都携带 HTTP Header。WebSocket 握手后是一条长期连接，本项目在 STOMP `CONNECT` 帧中携带 JWT，因此需要消息通道拦截器。

### 6.1 识别 CONNECT 帧

```java
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        // 只在建立 STOMP 会话时验证
    }
    return message;
}
```

`preSend` 表示消息进入通道前执行。`StompHeaderAccessor` 用于读取 STOMP 命令和 Header。

### 6.2 读取并验证 Token

```java
String authorization =
        accessor.getFirstNativeHeader("Authorization");

if (authorization == null
        || !authorization.startsWith("Bearer ")) {
    throw new AccessDeniedException(
            "WebSocket connection requires a JWT"
    );
}

String token = authorization.substring(7).trim();
String username = jwtService.extractUsername(token);
```

`Bearer ` 共 7 个字符，`substring(7)` 取出 JWT。`JwtService` 校验签名和有效期，并提取可信用户名。

### 6.3 绑定 Principal

```java
accessor.setUser(
        new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of()
        )
);
```

以后 Java 控制器可以从 `Principal` 获取登录用户，不需要相信浏览器提交的发送者名称。

答辩句：

> 身份来自服务端验证过的 JWT，并绑定到 WebSocket Principal；消息正文只提交接收者和内容，不能由客户端伪造发送者身份。

## 7. Java 接收 STOMP 消息

项目文件：`backend/src/main/java/com/earthonline/backend/chat/ChatWebSocketController.java`

```java
@Controller
public class ChatWebSocketController {
    private final ChatService chatService;

    public ChatWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat/messages")
    public void sendMessage(
            @Valid @Payload SendChatMessageRequest request,
            Principal principal
    ) {
        chatService.sendMessageAndPush(
                principal.getName(), request
        );
    }
}
```

因为应用前缀是 `/app`，完整发送地址是：

```text
/app + /chat/messages = /app/chat/messages
```

- `@Payload`：把 STOMP 正文转换成 Java DTO。
- `@Valid`：执行 DTO 参数校验。
- `Principal`：当前已认证用户。
- `principal.getName()`：JWT 中得到的可信用户名。
- `ChatService`：执行好友校验、落库与推送。

控制器保持简单，是因为业务逻辑应该放在 Service。

## 8. Java 校验并保存 MySQL

项目文件：`backend/src/main/java/com/earthonline/backend/chat/ChatService.java`

### 8.1 事务方法

```java
@Transactional
public ChatMessageResponse sendMessageAndPush(
        String currentUsername,
        SendChatMessageRequest request
) {
    ChatDelivery delivery = saveMessage(currentUsername, request);
    if (delivery.newlyCreated()) pushToReceiver(delivery);

    messagingTemplate.convertAndSendToUser(
            currentUsername,
            "/queue/chat/messages",
            delivery.senderResponse()
    );
    return delivery.senderResponse();
}
```

`@Transactional` 表示数据库操作处于一个事务中，发生运行时异常时可以回滚。

### 8.2 服务端权限校验

```java
UserEntity sender = findUserByUsername(currentUsername);
UserEntity receiver = findUserByPublicId(
        request.recipientPublicId()
);
requireFriendship(sender, receiver);
```

服务器根据认证用户名找发送者，根据公开地球 ID 找接收者，再检查两人是否为好友。前端隐藏按钮不等于安全，真正的权限校验必须在 Java 后端。

### 8.3 消息落库

```java
ChatMessageEntity message = chatMessageRepository.save(
        new ChatMessageEntity(
                sender,
                receiver,
                content,
                clientMessageId,
                messageType,
                imageObjectKey
        )
);
```

JPA 把 `ChatMessageEntity` 转成数据库记录。WebSocket 断开不会导致已经落库的聊天记录消失。

### 8.4 clientMessageId 去重

网络超时后用户可能重试。实体使用唯一约束：

```java
@UniqueConstraint(
    name = "uk_chat_sender_client_message",
    columnNames = {"sender_id", "client_message_id"}
)
```

Service 也会查询：

```java
chatMessageRepository.findBySenderIdAndClientMessageId(
        sender.getId(), clientMessageId
);
```

相同发送者沿用同一 `clientMessageId` 重试时返回已有消息，不重复插入。这叫幂等或去重。

## 9. Java 向指定用户实时推送

Spring 提供 `SimpMessagingTemplate`，业务代码不需要手工管理底层 Socket：

```java
messagingTemplate.convertAndSendToUser(
        delivery.receiverUsername(),
        "/queue/chat/messages",
        delivery.receiverResponse()
);
```

三个参数分别是接收者用户名、用户私人队列和要推送的 Java 对象。

浏览器订阅 `/user/queue/chat/messages`，Spring 根据 Principal 将它转换成该用户自己的内部目的地。A 和 B 即使订阅相同字符串，也只会收到属于自己的消息。

### 9.1 已读回执

打开聊天后，REST 接口把未读消息的 `readAt` 写入 MySQL，然后 Java 推送：

```java
messagingTemplate.convertAndSendToUser(
        friend.getUsername(),
        "/queue/chat/read-receipts",
        receipt
);
```

- MySQL 的 `read_at`：保存权威已读状态。
- WebSocket：实时通知对方更新“已读”显示。

## 10. 浏览器端需要理解到什么程度

答辩重点是 Java 后端，但仍应知道浏览器做了四件事：

1. 根据 HTTP/HTTPS 选择 `ws://` 或 `wss://`；
2. 在 STOMP CONNECT Header 中携带 JWT；
3. 订阅聊天、已读回执、好友事件三个私人队列；
4. 断线后每 5 秒重连，重新订阅并通过 REST 同步数据。

对应文件：`web/src/api/chatSocket.js`。不需要背完整 JavaScript，只需认识这些配置：

| 配置 | 含义 |
|---|---|
| `brokerURL` | WebSocket 地址 |
| `connectHeaders` | STOMP CONNECT 携带的 JWT |
| `reconnectDelay` | 重连间隔 |
| `heartbeatIncoming/Outgoing` | 心跳间隔 |
| `subscribe` | 订阅目的地 |

普通浏览器直接执行 JavaScript、HTML 和 CSS，Java 后端运行在服务器 JVM 中。前后端语言不同不影响双方按 WebSocket/STOMP 协议通信。

## 11. Nginx 为什么要特殊配置

项目文件：`deploy/nginx-earth-online.conf`

```nginx
location /ws/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 75s;
}
```

必须使用 HTTP/1.1，并转发 `Upgrade` 和 `Connection: upgrade`。如果 `/api` 正常但实时聊天失败，应检查 `/ws/` 代理、浏览器 Origin、JWT CONNECT 和订阅地址。

## 12. 一条消息的完整时序

### 12.1 建立连接

```text
1. 用户登录，浏览器获得 JWT。
2. Vue 创建 STOMP Client，连接 /ws/chat。
3. Nginx 转发 WebSocket Upgrade 到 Spring Boot。
4. 浏览器发送 STOMP CONNECT，并携带 Bearer JWT。
5. WebSocketJwtChannelInterceptor 验证 JWT。
6. Java 将用户名绑定为 Principal。
7. 浏览器订阅三个 /user/queue/** 目的地。
```

### 12.2 当前页面发送消息

```text
1. 浏览器生成 clientMessageId，先显示“发送中”。
2. 浏览器 POST /api/chat/messages。
3. JwtAuthenticationFilter 确认当前用户。
4. ChatController 调用 ChatService。
5. ChatService 检查接收者和好友关系。
6. 检查 clientMessageId，防止重复。
7. ChatMessageRepository.save 写入 MySQL。
8. SimpMessagingTemplate 推送给在线接收者。
9. REST 响应把权威消息返回发送者。
10. 页面替换本地“发送中”消息。
```

### 12.3 接收者离线怎么办

离线用户收不到当时的实时推送，但消息已经保存在 MySQL。下次进入聊天页面时，通过 REST 查询历史记录即可补齐：

> WebSocket 负责实时性，数据库负责可靠性，REST 负责查询和断线补偿。

## 13. 常见故障排查

### 13.1 REST 正常但 WebSocket 失败

1. 浏览器 Network 中 `/ws/chat` 是否返回 `101`；
2. HTTPS 网站是否使用 `wss://`；
3. Nginx 是否转发 `Upgrade` 和 `Connection`；
4. `setAllowedOriginPatterns` 是否包含实际网页来源；
5. STOMP CONNECT 是否携带有效 JWT；
6. 是否订阅正确的 `/user/queue/**` 地址。

### 13.2 能连接但收不到消息

- Java 是否执行 `convertAndSendToUser`；
- 接收用户名是否与 Principal 名称一致；
- 浏览器重连后是否重新订阅；
- 消息是否成功保存到 MySQL；
- 两个用户是否确实为好友。

### 13.3 消息重复

- 前端是否建立了多个连接或重复订阅；
- 重试是否沿用相同 `clientMessageId`；
- 数据库唯一约束是否存在。

## 14. 答辩高频问题

### Q1：为什么聊天不全部使用 HTTP？

HTTP 适合请求响应，服务器不能方便地主动通知在线用户。WebSocket 长连接可以实时推送新消息、已读回执和好友事件。

### Q2：为什么不全部使用 WebSocket？

历史查询、上传、CRUD 和明确的成功失败响应更适合 REST。项目使用 REST 落库与查询、WebSocket 实时推送，断线后还能通过 REST 恢复数据。

### Q3：WebSocket、STOMP、Spring 的关系？

WebSocket 是双向通信协议；STOMP 提供目的地、发送和订阅模型；Spring WebSocket 在 Java 后端完成端点、路由、鉴权和推送配置。

### Q4：如何防止伪造发送者？

发送者不从正文读取。后端验证 JWT 后把用户名绑定为 Principal，控制器从 `Principal.getName()` 取当前用户，Service 再验证好友关系。

### Q5：消息是 WebSocket 保存的吗？

不是。Java Service 调用 JPA Repository 将消息保存到 MySQL，WebSocket 只负责实时传输事件。

### Q6：断线后消息会不会丢？

实时事件可能错过，但已落库记录不会丢。重连后页面通过 REST 重新查询历史消息和未读状态。

### Q7：`/ws/chat` 与 `/app/chat/messages` 的区别？

前者是底层握手端点；后者是连接成功后发送 STOMP 聊天指令的业务目的地。

### Q8：为什么使用 `/user/queue/...`？

这是私人目的地。Spring 根据 Principal 路由，使用户只收到属于自己的消息，而不是广播给所有人。

### Q9：为什么需要 `clientMessageId`？

网络失败可能触发重试。相同发送者与 `clientMessageId` 的唯一约束可避免重复保存，实现幂等。

### Q10：能否支持多台后端服务器？

目前使用 Spring 内存 Simple Broker，适合单实例演示。多实例需要 Redis、RabbitMQ STOMP Broker Relay 等共享消息设施，并处理确认、重复、顺序和故障恢复。

## 15. 零基础代码阅读顺序

1. `WebSocketConfig.java`：端点和前缀；
2. `WebSocketJwtChannelInterceptor.java`：JWT 如何变成 Principal；
3. `ChatWebSocketController.java`：STOMP 如何进入 Java；
4. `SendChatMessageRequest.java`：输入字段；
5. `ChatService.java`：校验、去重、落库和推送；
6. `ChatMessageEntity.java`：数据库字段与唯一约束；
7. `ChatMessageRepository.java`：历史和未读查询；
8. `ChatController.java`：REST 与 WebSocket 分工；
9. 最后阅读 `chatSocket.js` 和 `FriendsView.vue`，只跟踪连接、订阅和页面刷新。

每读一个文件，回答：它接收什么、处理什么、把结果交给谁。

## 16. 自测练习

1. `/user`、`/app`、`/ws/chat` 分别是什么？
2. JWT 为什么要绑定为 Principal？
3. 在 `ChatService` 找到好友关系校验。
4. 找到消息真正写入 MySQL 的代码。
5. 找到推送给接收者的代码。
6. 找到防止消息重复的唯一约束。
7. 找到已读状态保存和推送的位置。

故障题：本地聊天正常，部署后历史消息能加载，但对方不能实时收到。

参考回答：

> 历史消息正常说明 REST、Java 后端和 MySQL 基本可用。实时接收失败应沿 WebSocket 链路排查：先看 `/ws/chat` 是否返回 101，再检查 Nginx Upgrade、部署域名或 IP 是否被 allowed origin 允许、STOMP CONNECT 的 JWT，以及私人队列是否成功订阅。

## 17. 一分钟答辩陈述模板

> Earth Online 的聊天采用 REST 与 WebSocket/STOMP 混合架构。浏览器通过 `/ws/chat` 建立连接，并在 STOMP CONNECT 阶段携带 JWT。Java 后端的通道拦截器验证 JWT 后将用户名绑定为 Principal，因此发送者身份不能由客户端伪造。当前页面使用 REST 提交消息，ChatService 校验双方好友关系，通过 clientMessageId 和数据库唯一约束避免重复，再用 JPA 将消息保存到 MySQL。保存成功后，Java 使用 SimpMessagingTemplate 向接收者的 `/user/queue/chat/messages` 私人目的地实时推送。已读回执和好友事件复用同一条连接。WebSocket 负责实时性，MySQL 负责持久化，REST 负责查询与断线补偿。

## 18. 必须牢记的十句话

1. WebSocket 是协议，不是 JavaScript 语言。
2. 本项目 WebSocket 服务端使用 Java 和 Spring Boot。
3. 浏览器客户端使用 JavaScript 是正常且必要的。
4. `/ws/chat` 是握手端点，不是消息目的地。
5. `/app/**` 进入 Java 的 `@MessageMapping`。
6. `/user/queue/**` 是登录用户的私人订阅。
7. JWT 在 STOMP CONNECT 阶段验证并绑定为 Principal。
8. 消息由 JPA 保存到 MySQL，不是由 WebSocket 保存。
9. 当前主要链路是 REST 发送、WebSocket 实时推送。
10. 断线重连后必须重新订阅，并用 REST 补齐权威数据。
