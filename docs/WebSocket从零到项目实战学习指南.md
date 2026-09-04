# WebSocket 从零到 Earth Online 项目实战

> 适合对象：不了解 HTTP、TCP、WebSocket 和 STOMP 的初学者  
> 学习方式：概念 → 图解 → 项目代码 → 动手实验 → 自测题 → 参考答案  
> 项目技术栈：Vue 3、`@stomp/stompjs`、Spring Boot、Spring WebSocket、JWT、MySQL

---

## 0. 如何使用这份文档

不要一次性背完所有概念。建议每学一章执行下面四步：

1. 阅读“核心概念”，尝试用自己的话复述。
2. 沿着给出的文件和行号阅读项目代码。
3. 完成“小实验”，在浏览器开发者工具中观察现象。
4. 先独立完成练习题，再查看答案。

推荐学习进度：

| 阶段 | 内容 | 建议时间 | 学完后的目标 |
|---|---|---:|---|
| 第一阶段 | HTTP、TCP、原生 WebSocket | 2～3 天 | 能解释为什么聊天需要长连接 |
| 第二阶段 | 握手、事件、关闭、STOMP | 2～3 天 | 能写一个最小聊天室并看懂 STOMP 地址 |
| 第三阶段 | 项目前后端连接与 JWT | 2～3 天 | 能说明用户如何建立经过认证的连接 |
| 第四阶段 | 落库、推送、幂等、重连 | 3～5 天 | 能完整追踪一条消息 |
| 第五阶段 | 多实例和外部 Broker | 1～2 天 | 知道单机方案为什么不能直接横向扩容 |

学习时建议准备两个不同账号，并分别登录两个浏览器窗口。Chrome 可以使用一个普通窗口加一个无痕窗口。

---

# 第一章：HTTP 请求/响应与 TCP 长连接

## 1.1 先认识网络分层

初学阶段可以只记住下面这条关系：

```text
聊天 JSON 数据
    ↓
STOMP（消息格式和目的地）
    ↓
WebSocket（双向通信协议）
    ↓
TCP（可靠、有序的字节传输）
    ↓
IP（负责把数据送到目标机器）
```

不同层解决不同问题：

- TCP 不理解“聊天消息”，它只负责可靠、有序地传输字节。
- WebSocket 规定浏览器和服务器如何建立全双工通信。
- STOMP 规定如何发送、订阅以及使用消息目的地。
- JSON 是项目中承载业务字段的数据格式。

不要把“WebSocket 连接”和“STOMP 连接”完全等同。项目先建立底层 WebSocket，再在这条连接上发送 STOMP `CONNECT` 帧。

## 1.2 HTTP 请求与响应

一次典型的 HTTP 请求：

```http
GET /api/chat/messages?friendPublicId=1234567890 HTTP/1.1
Host: localhost:5173
Authorization: Bearer eyJ...
```

服务器返回：

```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {"id": 1, "content": "你好"}
]
```

HTTP 的核心模型是：

```text
客户端发请求 → 服务器处理 → 服务器发响应
```

即使 HTTP 使用了 keep-alive，让底层 TCP 暂时不关闭，它的业务模型仍然是一次请求对应一次响应。服务器不能凭空通过某个旧 HTTP 响应持续通知浏览器。

## 1.3 短轮询为什么不理想

没有 WebSocket 时，可以每两秒请求一次：

```js
setInterval(async () => {
  const messages = await fetch('/api/chat/messages')
}, 2000)
```

这叫轮询。问题包括：

- 没有新消息也会发请求。
- 实时性取决于轮询间隔。
- 间隔越短，服务器和网络压力越大。
- 每次请求都有 HTTP Header 等额外开销。

## 1.4 TCP 长连接

TCP 建立连接后，只要双方不关闭，连接就能持续存在。它具有：

- 面向连接；
- 可靠传输；
- 按序到达；
- 双向传输。

“可靠”不代表应用永远不会丢消息。例如客户端断线期间，服务端向旧连接推送的实时事件不能自动到达客户端。因此应用仍需要消息落库、重连和状态补偿。

## 1.5 本章自测

1. HTTP keep-alive 和 WebSocket 长连接是一回事吗？
2. 为什么轮询间隔设置成 100 毫秒会增加服务器压力？
3. TCP 保证可靠传输，为什么聊天系统还需要把消息存进数据库？
4. 判断：WebSocket 能完全替代 HTTP。

<details>
<summary>查看参考答案</summary>

1. 不是。keep-alive 主要复用 TCP 连接，HTTP 仍按请求/响应工作；WebSocket 升级后允许双方主动发送消息。
2. 即使没有新数据，客户端仍会频繁建立请求、发送 Header、执行业务查询并返回结果。
3. TCP 只保证一条有效连接上的传输；用户可能离线、程序可能重启，历史消息和未读状态需要持久化。
4. 错。查询历史、上传文件、普通 CRUD 通常仍适合 HTTP；实时通知适合 WebSocket。

</details>

---

# 第二章：用原生 WebSocket 写最小聊天室

## 2.1 浏览器端最小代码

原生 WebSocket 客户端只有几个关键步骤：

```html
<!doctype html>
<html lang="zh-CN">
<body>
  <input id="message" placeholder="输入消息">
  <button id="send">发送</button>
  <ul id="messages"></ul>

  <script>
    const socket = new WebSocket('ws://localhost:8081')

    socket.addEventListener('open', () => {
      console.log('连接成功')
    })

    socket.addEventListener('message', event => {
      const item = document.createElement('li')
      item.textContent = event.data
      document.querySelector('#messages').appendChild(item)
    })

    socket.addEventListener('error', event => {
      console.error('连接发生错误', event)
    })

    socket.addEventListener('close', event => {
      console.log('连接关闭', event.code, event.reason)
    })

    document.querySelector('#send').addEventListener('click', () => {
      const content = document.querySelector('#message').value
      if (socket.readyState !== WebSocket.OPEN) {
        alert('连接尚未建立')
        return
      }
      socket.send(content)
    })
  </script>
</body>
</html>
```

这段代码涉及四个事件：

| 事件 | 触发时机 |
|---|---|
| `open` | 握手成功，可以发送消息 |
| `message` | 收到服务器消息 |
| `error` | 连接或传输发生错误 |
| `close` | 连接已经关闭 |

## 2.2 `readyState`

发送前应检查连接状态：

| 常量 | 数值 | 含义 |
|---|---:|---|
| `WebSocket.CONNECTING` | 0 | 正在连接 |
| `WebSocket.OPEN` | 1 | 已连接，可以发送 |
| `WebSocket.CLOSING` | 2 | 正在关闭 |
| `WebSocket.CLOSED` | 3 | 已关闭 |

如果在 `CONNECTING` 状态直接 `send()`，浏览器可能抛出异常。

## 2.3 文本和 JSON

真实聊天很少只发送一个字符串。通常会发送 JSON：

```js
socket.send(JSON.stringify({
  type: 'CHAT',
  recipientPublicId: '1234567890',
  content: '你好'
}))
```

收到消息时反序列化：

```js
const message = JSON.parse(event.data)
```

JSON 只是文本格式，不提供鉴权、路由、可靠存储或去重。

## 2.4 动手实验

可以先只做客户端思维实验，也可以用任意 WebSocket 测试服务器运行上述页面。依次尝试：

1. 连接成功之前点击发送。
2. 打开两个页面，观察广播消息。
3. 关闭服务器，观察 `error` 和 `close`。
4. 输出 `socket.readyState`。
5. 发送 JSON，并故意发送一个格式错误的字符串，练习 `try/catch`。

## 2.5 编程题

给收到消息的逻辑增加 JSON 解析保护。如果服务器发来的不是合法 JSON，页面不能崩溃，而是输出“未知消息”。

<details>
<summary>查看参考实现</summary>

```js
socket.addEventListener('message', event => {
  try {
    const message = JSON.parse(event.data)
    console.log('收到消息', message)
  } catch (error) {
    console.warn('未知消息', event.data)
  }
})
```

</details>

---

# 第三章：握手、ws/wss、消息与关闭

## 3.1 WebSocket 握手

WebSocket 最初通过 HTTP 发起协议升级：

```http
GET /ws/chat HTTP/1.1
Host: localhost:8080
Connection: Upgrade
Upgrade: websocket
Sec-WebSocket-Key: ...
Sec-WebSocket-Version: 13
```

服务器接受后返回：

```http
HTTP/1.1 101 Switching Protocols
Connection: Upgrade
Upgrade: websocket
Sec-WebSocket-Accept: ...
```

状态码 `101` 表示切换协议。之后传输的是 WebSocket 帧，不再是普通 HTTP 请求/响应。

## 3.2 `ws://` 与 `wss://`

| 页面 | WebSocket | 是否加密 |
|---|---|---|
| `http://` | `ws://` | 否 |
| `https://` | `wss://` | 是，经过 TLS |

HTTPS 页面连接 `ws://` 通常会被浏览器当作混合内容阻止。生产环境应使用 `wss://`。

项目会自动转换协议：

```js
apiUrl.protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:'
```

位置：`web/src/api/chatSocket.js`。

## 3.3 连接关闭

客户端可以主动关闭：

```js
socket.close(1000, '页面退出')
```

常见关闭码：

- `1000`：正常关闭；
- `1001`：页面离开或服务器关闭；
- `1006`：异常断开，通常不会作为正常关闭帧由应用发送；
- `1008`：违反策略，例如鉴权或权限不通过；
- `1011`：服务器内部错误。

页面组件销毁时应清理连接。本项目在 `FriendsView.vue` 的 `onBeforeUnmount` 中调用 `disconnectChatSocket()`。

## 3.4 反向代理为什么需要特殊配置

开发环境的 Vite 配置：

```js
'/ws': {
  target: 'ws://localhost:8080',
  ws: true,
  changeOrigin: true
}
```

生产环境 Nginx 配置：

```nginx
location /ws/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

若漏掉 `Upgrade` 和 `Connection`，REST 可能正常，但 WebSocket 握手失败。

## 3.5 本章自测

1. WebSocket 握手成功时常见的 HTTP 状态码是什么？
2. 网站使用 HTTPS 时，WebSocket 应使用什么协议？
3. 为什么关闭 Vue 页面前需要停用 WebSocket 客户端？
4. REST 正常但线上 WebSocket 失败，应该优先检查哪些配置？

<details>
<summary>查看参考答案</summary>

1. `101 Switching Protocols`。
2. `wss://`。
3. 防止连接、监听器和重连定时器继续存在，避免重复收消息和内存泄漏。
4. Nginx 的 HTTP/1.1、`Upgrade`、`Connection` 请求头，以及路径和 TLS 配置。

</details>

---

# 第四章：STOMP 的连接、发送、订阅和目的地

## 4.1 为什么项目还使用 STOMP

原生 WebSocket 只提供连接和收发数据，没有定义业务路由。假设应用同时有聊天、已读回执、好友事件，就需要自己设计消息类型和分发代码。

STOMP 提供类似消息队列的模型：

```text
客户端 SEND 到某个 destination
客户端 SUBSCRIBE 某个 destination
Broker 根据 destination 路由消息
```

常见 STOMP 命令包括：

- `CONNECT`：登录和建立 STOMP 会话；
- `CONNECTED`：服务器确认连接；
- `SEND`：发送消息；
- `SUBSCRIBE`：订阅目的地；
- `MESSAGE`：服务器向订阅者投递消息；
- `DISCONNECT`：结束会话；
- `ERROR`：协议或业务错误。

## 4.2 项目中的目的地

| 目的地 | 方向 | 用途 |
|---|---|---|
| `/ws/chat` | 浏览器 → 服务端 | 建立底层 WebSocket 连接，不是消息订阅地址 |
| `/app/chat/messages` | 客户端 → 应用 | 将聊天命令交给 Spring 控制器 |
| `/user/queue/chat/messages` | 服务端 → 当前用户 | 私聊消息 |
| `/user/queue/chat/read-receipts` | 服务端 → 当前用户 | 已读回执 |
| `/user/queue/friends/events` | 服务端 → 当前用户 | 好友关系事件 |

最容易犯的错误是把 `/ws/chat` 当成发送目的地。它只是握手入口。

## 4.3 连接和订阅

项目使用 `@stomp/stompjs`：

```js
const client = new Client({
  brokerURL: 'ws://localhost:5173/ws/chat',
  onConnect: () => {
    client.subscribe('/user/queue/chat/messages', frame => {
      const message = JSON.parse(frame.body)
      console.log(message)
    })
  }
})

client.activate()
```

必须在 `onConnect` 中订阅。连接断开并重连后，旧订阅通常已经失效，而 `onConnect` 会再次执行，从而重新订阅。

## 4.4 通过 STOMP 发送

项目已经提供纯 STOMP 发送函数：

```js
client.publish({
  destination: '/app/chat/messages',
  body: JSON.stringify({
    recipientPublicId,
    content,
    clientMessageId,
    messageType,
    imageObjectKey
  })
})
```

位置：`web/src/api/chatSocket.js` 的 `sendChatSocketMessage`。

Spring 会将 `/app` 去掉，再匹配：

```java
@MessageMapping("/chat/messages")
```

## 4.5 queue、topic 和 user

可以先使用下面的简化理解：

- `/topic/**`：偏向广播，一条消息可被多个订阅者收到；
- `/queue/**`：偏向点对点队列；
- `/user/queue/**`：Spring 为当前登录用户解析的私人目的地。

本项目使用私人用户队列，避免把私聊广播给所有在线用户。

## 4.6 本章练习

把下面地址分别归类为“握手入口”“应用发送地址”“用户订阅地址”：

1. `/ws/chat`
2. `/app/chat/messages`
3. `/user/queue/chat/messages`

<details>
<summary>查看参考答案</summary>

1. 握手入口。
2. 应用发送地址。
3. 用户订阅地址。

</details>

---

# 第五章：阅读前端 chatSocket.js

文件：`web/src/api/chatSocket.js`

## 5.1 地址计算

```js
const apiUrl = new URL(API_BASE_URL, window.location.origin)
const basePath = apiUrl.pathname.replace(/\/api\/?$/, '')
apiUrl.protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:'
apiUrl.pathname = `${basePath}/ws/chat`.replace(/\/{2,}/g, '/')
```

本地环境 `API_BASE_URL=/api` 时，大致会得到：

```text
ws://localhost:5173/ws/chat
```

Vite 再把 `/ws` 转发到 Spring Boot 的 `localhost:8080`。

## 5.2 防止重复连接

```js
if (client?.active) return
```

如果组件重复调用连接函数，不再创建第二个活跃客户端。这可以降低重复订阅和重复处理消息的风险。

## 5.3 每次连接前获取最新 JWT

```js
beforeConnect: () => {
  const token = getAccessToken()
  client.connectHeaders = token
    ? { Authorization: `Bearer ${token}` }
    : {}
}
```

放在 `beforeConnect` 而不是创建客户端时读取，意味着自动重连时能重新取得当前 Token。

## 5.4 三个订阅

```js
client.subscribe(CHAT_QUEUE, ...)
client.subscribe(CHAT_READ_RECEIPT_QUEUE, ...)
client.subscribe(FRIEND_EVENTS_QUEUE, ...)
```

每个订阅只负责一种事件，然后通过回调把业务交还给 `FriendsView.vue`。这样连接管理和页面 UI 逻辑彼此分离。

## 5.5 连接状态和重新同步

```js
const reconnected = hasConnected
hasConnected = true
onConnectionChange?.(true)
if (reconnected) onReconnect?.()
```

第一次连接不执行断线补偿；只有曾连接成功后再次连上，才调用 `onReconnect`。

## 5.6 阅读题

1. 为什么三个 `subscribe` 都写在 `onConnect` 里面？
2. `client.active` 和 `client.connected` 可以简单理解成什么区别？
3. 为什么消息正文要执行 `JSON.parse(frame.body)`？

<details>
<summary>查看参考答案</summary>

1. 必须连接成功才能订阅，而且重连成功时需要重新建立订阅。
2. `active` 表示客户端已启动并可能正在连接或重连；`connected` 表示当前 STOMP 会话已经连接成功。
3. STOMP 帧的正文是字符串，而页面需要使用 JavaScript 对象访问字段。

</details>

---

# 第六章：阅读后端 WebSocketConfig

文件：`backend/src/main/java/com/earthonline/backend/config/WebSocketConfig.java`

## 6.1 启用消息代理

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {
}
```

`@EnableWebSocketMessageBroker` 开启 Spring 的 WebSocket/STOMP 消息处理能力。

## 6.2 Broker 与应用前缀

```java
registry.enableSimpleBroker("/user");
registry.setApplicationDestinationPrefixes("/app");
registry.setUserDestinationPrefix("/user");
```

- 发往 `/app/**` 的消息交给带 `@MessageMapping` 的应用代码处理。
- `/user/**` 用于用户私人目的地。
- `enableSimpleBroker` 是 Spring 内存中的简单 Broker，适合当前单实例项目。

## 6.3 注册握手入口

```java
registry.addEndpoint("/ws/chat")
        .setAllowedOriginPatterns("http://localhost:5173");
```

这里还限制了允许的浏览器来源。部署时如果浏览器直接跨域连接其他域名，需要同步调整 Origin 配置。若前端与后端通过同域 Nginx 访问，通常更容易管理。

## 6.4 注册入站拦截器

```java
registration.interceptors(webSocketJwtChannelInterceptor);
```

浏览器发往服务器的 STOMP 帧会经过该拦截器，项目在其中检查 `CONNECT` 帧的 JWT。

## 6.5 配置题

假设客户端执行：

```js
client.publish({ destination: '/app/chat/messages' })
```

后端应该写哪个注解？

A. `@MessageMapping("/app/chat/messages")`  
B. `@MessageMapping("/chat/messages")`

<details>
<summary>查看参考答案</summary>

B。`/app` 是应用目的地前缀，Spring 路由时会去掉它，再匹配 `/chat/messages`。

</details>

---

# 第七章：JWT 如何变成 Principal

## 7.1 为什么不能相信消息里的 sender

下面的设计是不安全的：

```json
{
  "sender": "admin",
  "recipient": "user2",
  "content": "你好"
}
```

浏览器中的字段可以被用户修改。攻击者可以把 `sender` 改成任何名字。

正确做法是：

```text
JWT 验证身份
    ↓
服务端创建 Principal
    ↓
业务代码从 Principal 获取发送者
```

## 7.2 前端把 JWT 放入 CONNECT 帧

```js
client.connectHeaders = {
  Authorization: `Bearer ${token}`
}
```

这里是 STOMP 原生 Header，不是普通 REST 请求的 Header。

## 7.3 后端拦截 CONNECT

文件：`backend/src/main/java/com/earthonline/backend/auth/WebSocketJwtChannelInterceptor.java`

```java
if (StompCommand.CONNECT.equals(accessor.getCommand())) {
    String authorization =
            accessor.getFirstNativeHeader("Authorization");

    if (authorization == null
            || !authorization.startsWith("Bearer ")) {
        throw new AccessDeniedException(
                "WebSocket connection requires a JWT"
        );
    }
}
```

随后解析 Token 中的用户名：

```java
String token = authorization.substring(7).trim();
String username = jwtService.extractUsername(token);
```

最后绑定用户：

```java
accessor.setUser(
    new UsernamePasswordAuthenticationToken(
        username,
        null,
        List.of()
    )
);
```

## 7.4 控制器取得 Principal

文件：`backend/src/main/java/com/earthonline/backend/chat/ChatWebSocketController.java`

```java
@MessageMapping("/chat/messages")
public void sendMessage(
        @Valid @Payload SendChatMessageRequest request,
        Principal principal
) {
    chatService.sendMessageAndPush(
        principal.getName(),
        request
    );
}
```

因此 `principal.getName()` 才是可信发送者。

## 7.5 安全思考题

1. 握手 HTTP 请求成功，是否代表 STOMP 鉴权一定成功？
2. 为什么后端仍需检查双方是不是好友？
3. 如果 JWT 过期，自动重连可能发生什么？

<details>
<summary>查看参考答案</summary>

1. 不一定。本项目允许先建立底层连接，随后在 STOMP `CONNECT` 阶段验证 JWT。
2. 身份合法不代表拥有给任意用户发消息的业务权限。
3. `beforeConnect` 会重新读取 Token；若仍是过期 Token，CONNECT 会被拒绝。实际系统还需要刷新 Token 或重新登录策略。

</details>

---

# 第八章：跟踪一条消息如何落库并推送

## 8.1 先认清项目当前真实链路

当前 `FriendsView.vue` 发送消息调用的是 REST 函数：

```js
const savedMessage = await sendChatMessage(...)
```

`web/src/api/modules/chat.js` 中：

```js
request('/chat/messages', {
  method: 'POST',
  body: {
    recipientPublicId,
    content,
    clientMessageId,
    messageType,
    imageObjectKey
  }
})
```

所以当前架构是：

```text
发送者页面
  │
  │ POST /api/chat/messages
  ▼
ChatController
  ▼
ChatService.saveMessage
  ▼
MySQL 落库
  ├──────── REST 响应 ────────→ 发送者页面
  │
  └──────── WebSocket 推送 ───→ 接收者页面
```

这叫 REST + WebSocket 混合方案。

## 8.2 REST 控制器

```java
@PostMapping("/messages")
public ChatMessageResponse sendMessage(
        @Valid @RequestBody SendChatMessageRequest request,
        Authentication authentication
) {
    return chatService.sendMessage(
        authentication.getName(),
        request
    );
}
```

REST 中的身份来自 `Authentication`，WebSocket 中的身份来自 `Principal`，两者最终都把可信用户名传给同一个服务层。

## 8.3 保存前的校验

`ChatService.saveMessage` 会：

1. 查找发送者和接收者；
2. 检查双方是否为好友；
3. 检查文本或图片参数；
4. 检查 `clientMessageId` 是否已经使用；
5. 创建 `ChatMessageEntity`；
6. 调用 Repository 保存；
7. 分别构造发送者视角和接收者视角的响应。

为什么需要两个响应？因为同一条数据库消息在两端的 `mine` 字段不同：

- 发送者看到 `mine=true`；
- 接收者看到 `mine=false`。

## 8.4 给接收者推送

```java
messagingTemplate.convertAndSendToUser(
    delivery.receiverUsername(),
    "/queue/chat/messages",
    delivery.receiverResponse()
);
```

前端订阅的是：

```text
/user/queue/chat/messages
```

后端发送时没有写开头的 `/user`，这是 Spring 用户目的地的正常用法。Spring 会根据用户名和会话进行解析。

## 8.5 项目保留的纯 WebSocket 发送链路

`chatSocket.js` 还提供了 `sendChatSocketMessage()`：

```text
publish /app/chat/messages
  ↓
@MessageMapping("/chat/messages")
  ↓
sendMessageAndPush
  ↓
先落库
  ↓
分别推送给发送者和接收者
```

但当前页面没有调用该函数。学习时必须区分“代码具备此能力”和“当前 UI 实际正在使用”。

## 8.6 追踪题

用户 A 给用户 B 发送“你好”，请按顺序排列：

- `pushToReceiver`
- `FriendsView.deliverMessage`
- `ChatController.sendMessage`
- `ChatMessageRepository.save`
- B 的 `handleIncomingMessage`

<details>
<summary>查看参考答案</summary>

`FriendsView.deliverMessage` → `ChatController.sendMessage` → `ChatMessageRepository.save` → `pushToReceiver` → B 的 `handleIncomingMessage`。

</details>

---

# 第九章：clientMessageId、去重和失败重试

## 9.1 为什么数据库 ID 不够

数据库 ID 要等服务器保存后才生成。用户点击发送的瞬间，前端还没有数据库 ID，但页面希望立即显示“发送中”。因此前端先生成：

```js
const clientMessageId = crypto.randomUUID()
```

并创建临时消息：

```js
{
  id: `pending-${clientMessageId}`,
  clientMessageId,
  deliveryStatus: 'sending'
}
```

这叫乐观更新。

## 9.2 用正式消息替换临时消息

服务器返回或推送正式消息后，前端用 `clientMessageId` 查找临时消息：

```js
const pendingIndex = messages.value.findIndex(
  item => item.clientMessageId === message.clientMessageId
)
```

找到后替换为带数据库 ID 的正式消息。

## 9.3 幂等和去重

考虑这个场景：

```text
客户端发送消息
    ↓
服务器已经保存成功
    ↓
响应在网络中丢失
    ↓
客户端认为失败并重试
```

如果没有幂等处理，数据库会出现两条相同消息。

项目后端用“发送者 ID + clientMessageId”查询已有消息：

```java
findBySenderIdAndClientMessageId(
    sender.getId(),
    clientMessageId
)
```

数据库还设置唯一约束：

```java
@UniqueConstraint(
    name = "uk_chat_sender_client_message",
    columnNames = {"sender_id", "client_message_id"}
)
```

同一个发送者重试同一个 `clientMessageId` 时，返回原来的消息，而不是再次插入。

## 9.4 为什么唯一键包含 sender_id

不同用户可能碰巧生成相同的 `clientMessageId`。使用：

```text
(sender_id, client_message_id)
```

作为联合唯一键，使每个用户拥有自己的幂等命名空间。

## 9.5 超时和失败状态

项目定义了：

```js
const MESSAGE_TIMEOUT_MS = 10000
```

并支持三种 UI 状态：

- `sending`：正在发送；
- `sent`：服务器已经确认；
- `failed`：请求失败或超时，可以重试。

当前 REST 发送会直接等待 Promise 成功或失败。项目同时保留了消息确认超时相关代码，用于需要异步确认的发送方式。

## 9.6 场景题

一条消息实际上已经写入数据库，但客户端没收到响应。用户点击重试时，正确结果是什么？为什么？

<details>
<summary>查看参考答案</summary>

服务器应该返回第一次写入的消息，不再插入新记录。因为重试沿用同一个 `clientMessageId`，服务层会查到已有记录，数据库联合唯一约束也提供最后防线。

</details>

---

# 第十章：心跳、重连与断线补偿

## 10.1 为什么需要心跳

连接可能已经不可用，但两端暂时都不知道，例如：

- Wi-Fi 突然中断；
- 手机从 Wi-Fi 切换到流量；
- 路由器丢失连接状态；
- Nginx 清理空闲连接；
- 服务端进程崩溃。

心跳通过定期发送很小的数据来判断连接是否仍然活跃。

项目配置：

```js
heartbeatIncoming: 10000,
heartbeatOutgoing: 10000
```

可理解为双方约定大约每 10 秒确认一次活跃状态。

## 10.2 自动重连

```js
reconnectDelay: 5000
```

连接断开后，STOMP 客户端每 5 秒尝试重新连接。重连阶段要注意：

- 获取最新 JWT；
- 重新订阅；
- 避免重复客户端；
- 恢复页面连接状态；
- 补齐断线期间的数据。

## 10.3 为什么重连不等于数据恢复

假设用户在 10:00:00 断线，10:00:08 收到一条消息，10:00:15 重连。简单 Broker 不会自动把断线期间的实时帧永久保存并重新投递。

本项目重连后执行：

```js
await Promise.all([
  loadMessages(selectedFriend.value, { silent: true }),
  refreshUnreadCounts()
])
```

也就是通过 REST 重新读取数据库中的权威状态。这就是断线补偿。

## 10.4 重连风暴

如果服务器宕机，十万客户端都固定每 5 秒同时重连，会形成重连风暴。大型系统通常使用：

- 指数退避：1 秒、2 秒、4 秒、8 秒……；
- 最大等待时间；
- 随机抖动 jitter；
- 服务恢复后的限流。

当前项目固定 5 秒重连对于学习和小规模应用足够，但规模扩大后应优化。

## 10.5 动手实验

1. 登录两个账号并进入好友页面。
2. 打开开发者工具的 Network → WS。
3. 停止 Spring Boot，观察连接关闭。
4. 等待超过 5 秒，观察重连尝试。
5. 重新启动后端。
6. 检查是否重新出现订阅帧。
7. 检查当前会话和未读数是否通过 REST 得到补偿。

## 10.6 本章自测

1. 心跳能否保证消息永久保存？
2. 自动重连成功后，为什么还要查询历史消息？
3. 大规模系统为什么不宜让所有客户端固定同时重连？

<details>
<summary>查看参考答案</summary>

1. 不能。心跳只用于检测连接活性。
2. 断线期间的实时推送可能错过，数据库才是消息的权威状态。
3. 会瞬间产生大量连接和鉴权请求，拖慢或再次压垮刚恢复的服务器。

</details>

---

# 第十一章：已读回执和好友事件

## 11.1 已读状态为什么通过 REST 修改

用户打开会话时，页面调用：

```text
POST /api/chat/messages/read
```

后端将消息的 `readAt` 写入数据库，再通过 WebSocket 通知发送者。

这再次体现了项目的分工：

- REST：执行需要确认和持久化的状态修改；
- WebSocket：实时通知另一端状态已经改变。

## 11.2 批量回执

回执包含：

```text
readerPublicId
lastReadMessageId
readAt
```

发送者把 ID 小于等于 `lastReadMessageId` 的己方消息标为已读。这样不必给每条消息分别发送一次回执。

## 11.3 好友事件

项目还复用了同一条 STOMP 连接推送：

- `ACCEPTED`：好友申请被接受；
- `REJECTED`：好友申请被拒绝；
- `REMOVED`：好友关系被解除。

这说明一条 WebSocket 连接可以通过不同 STOMP 目的地承载多种业务事件。

---

# 第十二章：多实例部署与 Redis/RabbitMQ Broker

## 12.1 单实例为什么简单

当前架构可以简化为：

```text
浏览器 A ─┐
          ├─ Spring Boot 实例 ─ MySQL
浏览器 B ─┘
```

两个用户都连接同一个 Spring Boot 进程。进程内的简单 Broker 知道每条连接属于谁，因此能完成私人推送。

## 12.2 多实例会遇到什么问题

扩容后可能变成：

```text
用户 A ─ WebSocket ─ 实例 1
用户 B ─ WebSocket ─ 实例 2
```

如果 A 在实例 1 发送消息，而 B 的连接只存在于实例 2，实例 1 的内存 Broker 不知道如何直接把消息投递到实例 2 的连接。

只使用负载均衡的粘性会话可以缓解连接归属问题，但不能完整解决跨实例事件分发、故障转移和扩容问题。

## 12.3 外部消息系统的作用

可以引入共享的消息基础设施：

```text
实例 1 ─┐
        ├─ Redis Pub/Sub 或 RabbitMQ ─ 跨实例传递事件
实例 2 ─┘
```

### Redis

适合：

- 在线状态；
- 跨实例事件广播；
- 轻量 Pub/Sub；
- 缓存和会话辅助信息。

需要注意：传统 Redis Pub/Sub 更偏实时广播，订阅者离线时通常不会替它保存消息。可靠消费可进一步研究 Redis Streams。

### RabbitMQ

适合：

- 消息确认；
- 队列和交换机路由；
- 消费重试；
- 持久化消息；
- Spring STOMP Broker Relay。

Spring 可以使用外部 STOMP Broker Relay，把消息代理职责交给 RabbitMQ 等真正的消息代理。

## 12.4 数据库、Broker 与 WebSocket 的职责

| 组件 | 主要职责 |
|---|---|
| MySQL | 保存聊天记录、已读状态，作为业务权威数据 |
| Redis/RabbitMQ | 在服务实例之间传递事件或进行消息排队 |
| WebSocket | 把实时事件送到在线浏览器 |
| REST | 查询历史、确认写操作、断线后补偿 |

不能因为引入 RabbitMQ 就自动删除数据库消息表；两者解决的问题不同。

## 12.5 架构题

用户 A 连接实例 1，用户 B 连接实例 2。实例 1 保存消息后，怎样让实例 2 知道应该推送给 B？

<details>
<summary>查看参考答案</summary>

实例 1 将消息事件发布到所有实例共享的 Redis/RabbitMQ；实例 2 消费事件，发现 B 的 WebSocket 连接位于本机，再把消息推送给 B。具体实现还需要考虑确认、重复消息、顺序和失败重试。

</details>

---

# 第十三章：完整综合题

请先不看答案，尝试用文字完整回答。

## 题目 1：解释一次连接

用户进入好友页面后，从 Vue 组件挂载开始，到能够收到私人消息为止，发生了什么？

<details>
<summary>参考答案</summary>

1. `onMounted` 调用 `connectChatSocket`。
2. 前端计算 `/ws/chat` 的 `ws://` 或 `wss://` 地址。
3. 浏览器与服务器进行 WebSocket HTTP Upgrade 握手。
4. STOMP 客户端执行 `beforeConnect`，把 JWT 放进 CONNECT Header。
5. Spring 入站拦截器验证 JWT，将用户名绑定为 Principal。
6. STOMP 连接成功，触发前端 `onConnect`。
7. 前端订阅聊天消息、已读回执和好友事件三个私人目的地。
8. 服务端使用 `convertAndSendToUser` 时，Spring 根据 Principal/用户名把消息路由到该用户的连接。

</details>

## 题目 2：解释一次发送

当前项目中，A 给 B 发送文本后，A 和 B 分别怎样看到这条消息？

<details>
<summary>参考答案</summary>

1. A 的页面生成 `clientMessageId`，立即显示 `sending` 临时消息。
2. 页面通过 REST POST 发送消息。
3. 后端从 Authentication 获取 A，而不是相信浏览器提供的发送者字段。
4. 服务层验证 B、好友关系和消息内容。
5. 根据 `clientMessageId` 检查是否重复，然后落库。
6. REST 响应把正式消息返回给 A，A 用它替换临时消息。
7. 服务端通过 B 的私人 STOMP 队列实时推送接收者视角的消息。
8. B 的 `handleIncomingMessage` 更新好友预览、未读数或当前会话。

</details>

## 题目 3：故障分析

页面能查询历史消息，也能发送消息，但 B 必须刷新页面才能看到 A 的消息。列出至少五个检查点。

<details>
<summary>参考答案</summary>

1. 浏览器 Network → WS 中 `/ws/chat` 是否返回 101。
2. STOMP CONNECT 是否因 JWT 缺失或过期失败。
3. `onConnect` 是否执行，订阅帧是否成功发送。
4. B 是否订阅 `/user/queue/chat/messages`。
5. 后端是否执行 `convertAndSendToUser`，目标用户名是否正确。
6. Vite 或 Nginx 是否正确转发 `/ws`。
7. Nginx 是否设置 Upgrade 和 Connection Header。
8. 生产环境 HTTPS 页面是否错误连接 `ws://`。
9. Allowed Origin 是否允许当前前端来源。
10. 前端收到消息后是否因 JSON 解析或页面逻辑异常而未渲染。

</details>

## 题目 4：设计题

如果把当前 REST 发送改成纯 STOMP 发送，发送者怎样确认消息已经真正落库？

<details>
<summary>参考答案</summary>

不能把 `publish()` 执行成功当作消息已经落库。客户端先保留带 `clientMessageId` 的临时消息；服务端收到 STOMP SEND、完成校验和落库后，再向发送者的 `/user/queue/chat/messages` 推送权威消息。发送者根据相同 `clientMessageId` 替换临时消息。如果规定时间内未收到确认，则标记失败并允许使用同一 ID 重试。

</details>

---

# 第十四章：调试清单

## 浏览器侧

1. 打开开发者工具 → Network → WS。
2. 检查握手状态是否为 `101`。
3. 查看 Messages/Frames 中是否有 `CONNECT`、`CONNECTED`、`SUBSCRIBE`、`MESSAGE`。
4. 检查 CONNECT 是否携带 Authorization。
5. 检查订阅地址是否拼写正确。
6. 查看 Console 是否有 JSON 解析错误或 STOMP ERROR。
7. 查看断线后是否每 5 秒尝试重连。

## 服务端

1. `/ws/chat` 端点是否注册。
2. Origin 是否被允许。
3. JWT 拦截器是否收到 CONNECT。
4. `principal.getName()` 是否为预期用户。
5. `@MessageMapping` 是否与 `/app` 前缀正确对应。
6. 消息是否成功写入数据库。
7. `convertAndSendToUser` 的用户名是否正确。

## 代理层

1. `/ws/` 是否转发到正确端口。
2. 是否使用 HTTP/1.1。
3. 是否保留 `Upgrade` 和 `Connection` Header。
4. HTTPS 页面是否最终使用 WSS。
5. 代理空闲超时是否短于心跳策略。

---

# 第十五章：必须掌握的核心结论

学完后，应当能够不看资料解释下面这些话：

1. WebSocket 建立在 TCP 之上，通过 HTTP Upgrade 开始握手。
2. `ws://` 对应 HTTP，`wss://` 对应 HTTPS/TLS。
3. WebSocket 是传输协议，STOMP 提供发送、订阅和目的地模型。
4. `/ws/chat` 是连接入口，`/app/**` 是应用消息入口，`/user/**` 是用户私人目的地。
5. JWT 在 STOMP CONNECT 阶段验证，并被绑定为 Principal。
6. 发送者身份必须来自 Principal，不能相信前端传来的用户名。
7. 当前项目使用 REST 发送和落库，使用 WebSocket 实时推送。
8. `clientMessageId` 用于关联临时消息、发送确认、重试和幂等去重。
9. 心跳用于发现死连接，不负责保存消息。
10. 重连只恢复连接，断线补偿要重新读取数据库中的权威状态。
11. Spring Simple Broker 适合单实例；多实例需要跨节点事件分发方案。
12. MySQL、消息 Broker、WebSocket 和 REST 各有职责，不能互相简单替代。

---

# 附录：项目阅读路线

建议按下面顺序打开文件，不要一开始就通读整个好友页面：

1. `web/src/api/chatSocket.js`：连接、JWT、订阅、重连。
2. `web/src/api/modules/chat.js`：确认当前发送走 REST。
3. `backend/src/main/java/com/earthonline/backend/config/WebSocketConfig.java`：端点和目的地前缀。
4. `backend/src/main/java/com/earthonline/backend/auth/WebSocketJwtChannelInterceptor.java`：JWT → Principal。
5. `backend/src/main/java/com/earthonline/backend/chat/ChatWebSocketController.java`：STOMP 应用入口。
6. `backend/src/main/java/com/earthonline/backend/chat/ChatController.java`：当前 REST 入口。
7. `backend/src/main/java/com/earthonline/backend/chat/ChatService.java`：校验、落库、推送和回执。
8. `backend/src/main/java/com/earthonline/backend/chat/ChatMessageEntity.java`：消息字段和唯一约束。
9. `web/src/views/FriendsView.vue`：临时消息、接收处理、已读和断线补偿。
10. `web/vite.config.js` 与 `deploy/nginx-earth-online.conf`：开发和生产代理。

完成这份路线后，可以尝试独立画出下面三张图：

- WebSocket/STOMP 建连时序图；
- 当前 REST 发送 + WebSocket 接收时序图；
- 多实例 + RabbitMQ/Redis 的事件流转图。

