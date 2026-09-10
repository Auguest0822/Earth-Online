# 聊天实时推送失效与消息提示音不响排障记录

> 记录日期：2026-09-11  
> 适用项目：Earth Online（Vue 3 + Spring Boot + WebSocket/STOMP + MySQL）  
> 涉及文件：`WebSocketConfig.java`、`WebSocketJwtChannelInterceptor.java`、`web/src/stores/chatNotifications.js`  
> 相关文档：[`WebSocket从零到项目实战学习指南.md`](./WebSocket从零到项目实战学习指南.md)、[`08-好友聊天与WebSocket.md`](./code-reading-guide/08-好友聊天与WebSocket.md)

## 0. 结论速览

好友聊天“要刷新页面才能看到新消息”，而且“消息提示音不响”。看起来像前端订阅或渲染的问题，实际**两个根因都在后端的 Spring WebSocket 配置与鉴权**，前端只是被动受害者。

| # | 现象 | 真正的根因 | 修复 |
| --- | --- | --- | --- |
| 1 | 对方收不到实时消息（刷新后能查到） | `enableSimpleBroker("/user")` 写错了代理前缀，Spring 把翻译后的 `/queue/xxx-user{sessionId}` 目的地**静默丢弃** | 改为 `enableSimpleBroker("/queue", "/topic")` |
| 2 | 未读红点、已读回执、好友事件也不实时 | 同上（都用同一条私人队列推送链路） | 同上 |
| 3 | 提示音不响 | 提示音只在“收到实时推送”时触发；根因 1 修好后自然恢复。另外原解锁逻辑有硬门禁，做了加固 | `chatNotifications.js` 解锁与播放逻辑加固 |
| 4 | 用 `http://127.0.0.1:5173` 打开页面时连接失败 | `localhost` 与 `127.0.0.1` 属于不同 Origin，握手被 `setAllowedOriginPatterns` 拒绝 | 默认白名单同时允许两者 |

修复后已用两个真实账号在后端实测：**接收方无需刷新即可收到消息，已读回执也实时回流**（见第 7 节实测输出）。

---

## 1. 现象与影响范围

1. A 给好友 B 发消息，B 的聊天窗口**没有任何新消息**，左侧红点也不动；
2. B 手动刷新页面（或重新进入会话）后，消息、红点、已读状态全部正常；
3. 消息提示音从来不响；
4. 前后端控制台**没有明显报错**，只有一条容易被忽略的 WebSocket 连接相关日志。

第 2 条非常关键：刷新后能看到消息，说明 **REST 查询 + MySQL 落库都是好的**，坏的只是“实时推送”这一条支路。项目的聊天是 REST 与 WebSocket 混合架构：

```text
发送方页面 ──REST POST /api/chat/messages──▶ Spring Boot ──JPA──▶ MySQL（权威数据，刷新可见）
                                                  │
                                        SimpMessagingTemplate
                                                  │  实时推送（坏的就是这里）
                                                  ▼
                                    接收方 /user/queue/chat/messages
```

因此排查范围可以直接锁定在 WebSocket 推送链路：连接是否建立 → 订阅是否注册 → 用户是否绑定 → 服务端推送是否解析到会话。

---

## 2. 排查过程

### 2.1 先确认“连接”和“订阅”有没有成功

前端 `web/src/api/chatSocket.js` 在 `onConnect` 里订阅三个私人目的地：

```js
client.subscribe('/user/queue/chat/messages', ...)        // 新消息
client.subscribe('/user/queue/chat/read-receipts', ...)   // 已读回执
client.subscribe('/user/queue/friends/events', ...)       // 好友事件
```

打开后端 TRACE 日志（关键一步）：

```bash
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar \
  --logging.level.org.springframework.messaging=TRACE
```

日志里能看到两次“翻译”，说明订阅其实是**注册成功**的：

```text
TRACE o.s.m.s.u.UserDestinationMessageHandler : Translated /user/queue/chat/messages -> [/queue/chat/messages-user<c7f1081...>]
DEBUG o.s.m.s.b.SimpleBrokerMessageHandler    : Processing SUBSCRIBE destination=/queue/chat/messages-user<c7f1081...> subscriptionId=sub-0
```

但紧接着推送时报了另一条：

```text
TRACE o.s.m.s.u.UserDestinationMessageHandler : No active sessions for user destination: /user/<接收方用户名>/queue/chat/messages
```

这两条日志正好对应两个独立的 Bug：

- 第一条链路说明：**目的地在代理内部会被改写成 `/queue/...-user{sessionId}`**；
- 第二条说明：按“用户名”推送时，Spring 的在线用户表里**查不到这个用户**，于是 `<无会话>` → 消息被丢弃。

### 2.2 顺手验证的一个坑：`enableSimpleBroker("/user")` 为什么致命

带着上面的第一条日志去翻 Spring 源码（`spring-messaging 7.0.8`），链路是：

| 步骤 | 类 / 方法 | 行为 |
| --- | --- | --- |
| 1 | `DefaultUserDestinationResolver#getTargetDestination` | 把 `/user/queue/chat/messages` 变成 `/queue/chat/messages-user{sessionId}` |
| 2 | `SimpleBrokerMessageHandler#handleMessageInternal` | 先执行 `if (!checkDestinationPrefix(destination)) return;` |
| 3 | `AbstractBrokerMessageHandler#checkDestinationPrefix` | 目的地必须以 `enableSimpleBroker(...)` 配置的前缀开头，否则整条消息直接丢弃 |

也就是说：**订阅（SUBSCRIBE）和投递（MESSAGE）两个环节都要过这道前缀检查**，而翻译后的目的地是 `/queue/...` 开头。当配置写成 `enableSimpleBroker("/user")` 时：

- 真正需要被注册的 `/queue/chat/messages-user{id}` → 前缀不匹配 → **订阅被丢弃**；
- 应用推送时翻译出来的 `/queue/chat/messages-user{id}` → 前缀不匹配 → **消息被丢弃**。

整个过程不抛异常、不打错误日志，这就是“聊天像坏了但又没报错”的原因。

---

## 3. 根因一：代理前缀写成 `/user`，私人消息被静默丢弃

### 3.1 出错代码（修复前）

文件：`backend/src/main/java/com/earthonline/backend/config/WebSocketConfig.java`

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/user");          // ✗ 代理前缀写成了用户前缀
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
}
```

`/user` 是**用户目的地前缀**，不是**代理处理的目的地前缀**。两者混用后，代理只认 `/user`，而内部翻译结果落在 `/queue`，于是全部落空。

### 3.2 修复后

```java
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
```

### 3.3 一句话记忆

> `setUserDestinationPrefix("/user")` 决定“浏览器写地址时用哪个前缀”；  
> `enableSimpleBroker("/queue", "/topic")` 决定“内存代理真正处理哪些前缀”。  
> 前者是给客户端看的门牌号，后者是代理自己的收件范围，**不能混为一谈**。

---

## 4. 根因二：`StompHeaderAccessor.wrap()` 返回副本，用户没绑定到会话

### 4.1 出错代码（修复前）

文件：`backend/src/main/java/com/earthonline/backend/auth/WebSocketJwtChannelInterceptor.java`

```java
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);   // ✗ 这是一份副本

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        // ... 校验 JWT ...
        accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, List.of()));  // ✗ 写进了副本
    }
    return message;
}
```

### 4.2 为什么这样写“看起来是好的”

`StompHeaderAccessor.wrap(message)` 的实现是：

```java
public static StompHeaderAccessor wrap(Message<?> message) {
    return new StompHeaderAccessor(message);   // spring-messaging 7.0.8 源码
}
```

它**新建**了一个 accessor，复制了原消息的 Header。于是：

- **读** Header 完全正常 → JWT 校验通过，握手成功，界面看不出问题；
- **写** `setUser` 写的是副本自己的 Header → 原消息没变。

更关键的是 `SimpMessageHeaderAccessor` 的这两个方法：

```java
public void setUser(@Nullable Principal principal) {
    setHeader(USER_HEADER, principal);
    if (this.userCallback != null) {
        this.userCallback.accept(principal);   // 只有原 accessor 上才注册过这个回调
    }
}

/**
 * ... This is used internally on the inbound channel to detect
 * token-based authentications through an interceptor.
 */
public void setUserChangeCallback(Consumer<Principal> callback) { ... }
```

而 `userChangeCallback` 是 `StompSubProtocolHandler` 在收到 CONNECT 时注册到**原 accessor** 上的：

```java
info = new SessionInfo(channelToUse, session.getPrincipal());
headerAccessor.setUserChangeCallback(info);   // 绑定“用户变化 → 更新会话信息”
...
outputChannel.send(message);                  // 然后消息才进入 clientInboundChannel，被拦截器处理
```

副本上没有这个回调，所以 `setUser` 之后：

```text
消息头里没有 simpUser
      ↓
WebSocket 会话信息（SessionInfo）里没有 Principal
      ↓
SimpUserRegistry（在线用户表）里没有这个用户
      ↓
convertAndSendToUser(username, "/queue/chat/messages", payload)
      ↓
DefaultUserDestinationResolver 按用户名查会话 → 0 个会话
      ↓
UserDestinationMessageHandler: "No active sessions for user destination: ..."
      ↓
消息被丢弃（不报错）
```

同一个 Bug 还会让 `@MessageMapping` 方法里的 `Principal principal` 变成 `null`，即 `/app/chat/messages` 这条 STOMP 发送路径也是坏的。

### 4.3 修复后

```java
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
        // ... 校验 JWT ...
        accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, List.of()));
    }
    return message;
}
```

要点：用 `MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)` 取回**消息原本的** accessor，它和消息头是同一份数据，`setUser` 才会同时写进消息头并触发回调。

### 4.4 规范写法对照

| 写法 | 能否读 Header | `setUser` 是否生效 | 会话是否拿到 Principal |
| --- | --- | --- | --- |
| `StompHeaderAccessor.wrap(message)` | ✅ | ❌（写进副本） | ❌ |
| `MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class)` | ✅ | ✅（写回消息头并触发回调） | ✅ |

> 补充：Spring 默认给 `clientInboundChannel` 注册了 `ImmutableMessageChannelInterceptor`，它会**在拦截器链末尾**才把消息头设为不可变，正是为了让 CONNECT 拦截器有机会 `setUser`。所以只要拿对 accessor，`setUser` 就是官方推荐用法。

---

## 5. 为什么一开始容易误判成前端问题

1. **症状指向 UI**：“要刷新才看到新消息”天然让人怀疑 Vue 的响应式、订阅回调或 `messages` 列表渲染；
2. **前端确实曾经背过锅**：历史提交 `3212c41 修复移动端头像展示和聊天更新需刷新页面的问题` 只改了前端，问题却没消失，因为主因在服务端；
3. **没有任何报错**：目的地前缀不匹配属于“静默丢弃”，`connectChatSocket` 的 `onConnect` 正常触发，STOMP 订阅也拿到了 `receipt`；
4. **提示音是连带症状**：`playNotificationSound()` 只在 `chatNotifications.js` 的 `onMessage`（收到实时推送）里调用，推送被丢弃，声音自然永远不响——它**不是**独立故障。

经验：遇到“WebSocket 能连上但收不到消息”，先看**服务端**日志有没有 `Translated ... -> [...]` 与 `No active sessions for user destination` 这两类行，比反复改前端有效得多。

---

## 6. 顺带修复的两个小问题

### 6.1 提示音解锁逻辑加固

文件：`web/src/stores/chatNotifications.js`

原来的写法用布尔量 `soundUnlocked` 做硬门禁，并且只监听一次冒泡阶段的 `pointerdown`/`keydown`，一旦首次交互被 `stopPropagation` 吞掉或时序不对，就永远播不出声音。

修复后：

- 去掉硬门禁，改为“`running` 就直接播，`suspended` 就 `resume()` 后再播”，被浏览器策略拦截时保持静默，不影响未读红点；
- 解锁监听改成**捕获阶段**并覆盖 `pointerdown` / `keydown` / `touchstart`：

```js
const bindSoundUnlockHandlers = () => {
  if (soundUnlockHandlersBound) return
  soundUnlockHandlersBound = true
  // 捕获阶段监听，避免子元素 stopPropagation 使解锁失效。
  const options = { capture: true, passive: true, once: true }
  ;['pointerdown', 'keydown', 'touchstart'].forEach(type => {
    window.addEventListener(type, unlockNotificationSound, options)
  })
}
```

> 浏览器自动播放策略要求“页面至少发生过一次用户交互”，所以**打开页面后必须点击或按键一次**，之后的实时消息才会响铃。这是浏览器限制，不是 Bug。

### 6.2 本地 Origin 白名单

`localhost` 和 `127.0.0.1` 是两个不同的 Origin。原来只允许 `http://localhost:5173`，如果浏览器地址栏是 `http://127.0.0.1:5173`，握手会被直接拒绝（403），也表现为“收不到实时消息”。

```yaml
# application.yaml
chat:
  websocket:
    # localhost 与 127.0.0.1 属于不同 Origin，本地测试两个都要保留。
    allowed-origin-patterns: ${CHAT_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS:http://localhost:5173,http://127.0.0.1:5173}
```

部署时用环境变量覆盖成真实站点地址（HTTP 与 HTTPS 也要分别列出）。

---

## 7. 验证方式与实测结果

### 7.1 服务端日志（修复后应看到）

```text
TRACE UserDestinationMessageHandler : Translated /user/queue/chat/messages -> [/queue/chat/messages-user<c7f1081...>]
DEBUG SimpleBrokerMessageHandler    : Processing SUBSCRIBE destination=/queue/chat/messages-user<c7f1081...> subscriptionId=sub-0
TRACE UserDestinationMessageHandler : Translated /user/<接收方用户名>/queue/chat/messages -> [/queue/chat/messages-user<52b72db3...>]
```

重点是**不再出现** `No active sessions for user destination`。

### 7.2 双账号端到端脚本

完整脚本（临时验证用，可保留也可删除）：`output/chat-ws-verify.mjs`。用法：

```bash
# 1) 先启动 MySQL 与后端（8080）
# 2) 再执行
node output/chat-ws-verify.mjs
```

脚本做的事：注册/登录两个临时账号（`wsv*`）→ 互加好友 → 两个 STOMP 客户端带 JWT 连接 `/ws/chat` 并订阅私人队列 → 一方 REST 发消息 → 断言另一方实时收到 → 再验证已读回执与 `/app/chat/messages` 发送路径。脚本末尾会打印临时账号名，便于清理（清理 SQL 见 7.4）。

实测输出：

```text
账号 A=wsv575572a/7977617693  B=wsv575572b/6760559901
已成为好友，开始 WebSocket 实时推送验证
两端 STOMP CONNECTED 并订阅 /user/queue/chat/messages
A 已通过 REST 发送消息，等待 B 的 WebSocket 推送…
✅ B 通过 WebSocket 收到消息: {"id":26,"content":"实时推送验证 575572","mine":false}
✅ A 通过 WebSocket 收到已读回执: {"readerPublicId":"6760559901","lastReadMessageId":26,"readAt":"2026-09-11T07:22:56.1707983"}
✅ /app/chat/messages 路径：A 收到消息、B 收到自己的回显（Principal 绑定正确）
---
验证通过：WebSocket 实时推送与已读回执均正常工作
```

三条链路全部打通：

1. REST 发送 → 接收方**实时**收到（对应原现象“要刷新才看到”）；
2. 已读回执 → 发送方**实时**收到；
3. `/app/chat/messages`（STOMP 发送）→ 双方都收到，证明 `Principal` 绑定正确。

### 7.3 浏览器手工验证

1. 重启后端（改的是后端代码，必须重启）；
2. 用两个浏览器窗口 / 一个普通窗口 + 一个隐身窗口，分别登录两个互为好友的账号；
3. 双方都进入「朋友」页面，并**随意点击一次页面**（解锁音频）；
4. A 发送消息：B 不刷新即可看到新消息、左侧未读数 +1、并有提示音；
5. B 打开该会话：A 的消息出现已读状态（已读回执实时回流）。

### 7.4 清理临时测试账号

```sql
delete cm from chat_messages cm join users u on (cm.sender_id = u.id or cm.receiver_id = u.id) where u.username like 'wsv%';
delete fr from friend_requests fr join users u on (fr.sender_id = u.id or fr.receiver_id = u.id) where u.username like 'wsv%';
delete from users where username like 'wsv%';
```

---

## 8. 本地复测环境备忘

- 后端环境变量（IDEA 运行配置里已有）：`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`MINIO_*`；
- MySQL 若未启动：服务方式 `net start MySQL`（需要管理员），或直接运行 `E:\mysql\start_mysql.bat`；
- 只改前端时 Vite 会热更新；**改了后端必须重新编译并重启 Spring Boot**；
- 打包命令：`mvn -f backend/pom.xml -DskipTests package`，产物 `backend/target/backend-0.0.1-SNAPSHOT.jar`。

---

## 9. 答辩问答（可以直接背）

**Q1：消息明明保存进 MySQL 了，为什么对方收不到？**  
> 本项目是 REST + WebSocket 混合架构：REST 负责请求、落库和查询，WebSocket 只负责实时推送。消息落库成功只证明 REST 与 MySQL 正常；实时推送经过 `SimpMessagingTemplate` → 用户目的地解析 → 内存代理三步，本次故障出在第三步：代理的目的地前缀配置错误，翻译后的 `/queue/...-user{sessionId}` 不匹配任何代理前缀，被 Spring 静默丢弃。

**Q2：为什么整条链路一个错误都不报？**  
> Spring 的 `SimpleBrokerMessageHandler` 用 `checkDestinationPrefix` 过滤目的地，不匹配就直接 `return`，属于“静默丢弃”；而用户目的地解析不到会话时，`UserDestinationMessageHandler` 也只打 TRACE 日志。所以必须打开 `logging.level.org.springframework.messaging=TRACE` 才能看到 `Translated ...` 与 `No active sessions for user destination` 这两条关键日志。

**Q3：`/user`、`/app`、`/queue` 分别在做什么？**  
> `/ws/chat` 是握手端点；`/app` 是客户端发给 `@MessageMapping` 的应用前缀；`/user` 是用户目的地前缀，客户端用 `/user/queue/xxx` 表示“发给我自己”；`/queue`、`/topic` 是内存代理真正处理的目的地前缀。用户目的地最终会被翻译成 `/queue/xxx-user{sessionId}` 再进入代理，所以 `enableSimpleBroker` 必须包含 `/queue`。

**Q4：STOMP 的 JWT 是怎么变成 Principal 的？**  
> 浏览器在 CONNECT 帧头里带 `Authorization: Bearer <token>`，`WebSocketJwtChannelInterceptor` 在 `clientInboundChannel` 上解析并 `setUser`。这里必须用 `MessageHeaderAccessor.getAccessor(...)` 拿消息原本的 accessor：只有它才会同时写回消息头并触发 `StompSubProtocolHandler` 注册的 `userChangeCallback`，从而把用户登记进 `SimpUserRegistry`，`convertAndSendToUser(用户名, ...)` 才能解析到具体会话。用 `StompHeaderAccessor.wrap(message)` 拿到的是副本，`setUser` 会丢失。

**Q5：消息提示音为什么之前不响？**  
> 提示音在 Pinia 的 `chatNotifications` store 里，由“收到实时推送”这个事件触发，所以推送链路断了它必然不响。修好推送后，只需要注意浏览器的自动播放策略：页面必须发生过一次用户交互，代码在首次 `pointerdown/keydown/touchstart`（捕获阶段）解锁 `AudioContext`。

---

## 10. 经验总结

1. **“要刷新才能看到”= 写入成功、推送失败**，优先怀疑推送链路而不是数据库和前端渲染；
2. **客户端订阅成功 ≠ 服务端会投递**，中间还有“目的地翻译 + 代理前缀过滤 + 用户会话解析”三关；
3. **前缀不能混用**：用户前缀 `/user` 与代理前缀 `/queue`、`/topic` 是两套语义；
4. **STOMP 拦截器改 Header 要用原 accessor**，`wrap()` 是副本，写进去会丢；
5. **没有报错不等于链路正常**，`TRACE` 日志是排查消息中间件类问题的第一手段。
