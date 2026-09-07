# 08 好友聊天与 WebSocket

## REST 与 WebSocket 的分工

```text
REST：查询历史、未读数、保存消息的备用路径、标记已读
WebSocket/STOMP：实时发送新消息、已读回执、好友事件
MySQL：永久保存聊天消息
```

WebSocket 不是数据库。断开连接后历史消息仍能恢复，是因为消息保存到了 MySQL。

## STOMP 连接地址

```js
apiUrl.protocol = apiUrl.protocol === 'https:' ? 'wss:' : 'ws:'
apiUrl.pathname = `${basePath}/ws/chat`
```

HTTPS 页面必须使用 WSS，避免混合内容被浏览器拦截。STOMP 在 WebSocket 之上提供 destination、subscribe、publish 等消息语义。

## CONNECT 阶段携带 JWT

```js
beforeConnect: () => {
  const token = getAccessToken()
  client.connectHeaders = token
      ? { Authorization: `Bearer ${token}` } : {}
}
```

浏览器原生 WebSocket 握手不方便加入任意 Authorization Header，因此 Token 放入 STOMP CONNECT 帧。后端 `WebSocketJwtChannelInterceptor` 在 CONNECT 阶段解析 Token，并把用户身份绑定到 WebSocket 会话。

## 私有订阅

```js
client.subscribe('/user/queue/chat/messages', onMessage)
client.subscribe('/user/queue/chat/read-receipts', onReadReceipt)
client.subscribe('/user/queue/friends/events', onFriendEvent)
```

`/user/queue/...` 是用户私有目的地。后端使用：

```java
messagingTemplate.convertAndSendToUser(
    receiverUsername,
    "/queue/chat/messages",
    receiverResponse
);
```

Spring 根据已认证用户名把消息路由到该用户的会话，不需要前端订阅包含用户名的公共频道。

## 发送消息的权威路径

```text
前端 publish /app/chat/messages
  → ChatWebSocketController
  → ChatService.saveMessage
  → 验证双方是好友
  → 保存 ChatMessageEntity
  → 分别推送给发送者与接收者
```

为什么还推送给发送者？服务端响应含数据库 ID、最终时间和规范化数据，是“权威副本”，可以替换前端临时消息。

## 好友权限

```java
boolean friends = friendRequestRepository
    .findRelationshipsBetween(currentUser.getId(), friend.getId())
    .stream()
    .anyMatch(request -> request.getStatus() == FriendRequestStatus.ACCEPTED);
if (!friends) throw new ResponseStatusException(
    HttpStatus.FORBIDDEN, "Only friends can chat");
```

前端是否显示聊天框不是安全依据；后端每次查询、发送和标记已读都要验证好友关系。

## clientMessageId 与幂等

```java
ChatMessageEntity existing = chatMessageRepository
    .findBySenderIdAndClientMessageId(sender.getId(), clientMessageId)
    .orElse(null);
```

网络重连可能让同一条消息重复发送。客户端生成稳定 ID，服务端发现已有消息就返回旧记录而不再次保存。数据库唯一约束仍是并发情况下的最终防线。

## 断线重连

```js
reconnectDelay: 5000,
heartbeatIncoming: 10000,
heartbeatOutgoing: 10000
```

每 5 秒自动重连；心跳用于发现半开连接。重连成功后执行 `onReconnect`，页面应重新拉取历史和未读状态，补齐断线期间遗漏的实时事件。

## 评委可能追问

**为什么不只用 WebSocket？** REST 更适合历史查询、幂等资源操作和断线恢复；WebSocket 适合服务端主动推送，两者互补。

**怎样防止给陌生人发消息？** Service 基于数据库中已接受的好友关系判断，而不是相信前端选择状态。

