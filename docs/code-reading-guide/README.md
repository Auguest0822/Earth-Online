# Earth Online 答辩源码旁注指南

这套文档是“放在代码旁边看的注释版教材”。它不会修改 `web/` 或 `backend/` 中的任何源码，而是摘录项目真实代码，在 Markdown 中逐段解释语法、运行顺序、设计原因、数据去向和答辩追问。

## 阅读顺序

1. [`01-项目启动与前端骨架.md`](01-项目启动与前端骨架.md)
2. [`02-登录注册与JWT鉴权.md`](02-登录注册与JWT鉴权.md)
3. [`03-Pinia状态管理与HTTP请求.md`](03-Pinia状态管理与HTTP请求.md)
4. [`04-生活事记CRUD与JPA.md`](04-生活事记CRUD与JPA.md)
5. [`05-MinIO图片上传与降级.md`](05-MinIO图片上传与降级.md)
6. [`06-DeepSeek年度总结.md`](06-DeepSeek年度总结.md)
7. [`07-RAG年度回忆问答.md`](07-RAG年度回忆问答.md)
8. [`08-好友聊天与WebSocket.md`](08-好友聊天与WebSocket.md)
9. [`09-未来信件与业务规则.md`](09-未来信件与业务规则.md)

## 看代码时固定回答五个问题

1. 用户从哪个页面触发？
2. 前端调用哪个 API？
3. 哪个 Controller 接收？
4. Service 做了什么业务和权限判断？
5. 最终写入哪个表或调用哪个外部服务？

## 答辩表述原则

- 密码由 BCrypt 哈希，JWT 负责身份声明和签名验证，二者不是一回事。
- Vue Router 守卫只改善交互，真正的接口安全由 Spring Security 和用户级查询保证。
- MySQL 保存结构化数据和 MinIO 对象键；图片二进制存入 MinIO。
- WebSocket 负责实时传输，MySQL 负责消息持久化。
- 当前 RAG 是字段匹配与加权排序，不是向量数据库，也没有训练 DeepSeek。

