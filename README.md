# Earth Online

Earth Online 是一个记录个人生活轨迹的全栈项目，包含时间线、年度回顾、未来信件、好友与实时聊天。

## 功能

- 注册、登录与 JWT 身份认证
- 个人档案与头像管理
- 生活事记、心情、标签和图片记录
- 时间线与年度数据分析
- DeepSeek AI 年度总结与带原始记录引用的 RAG 年度回忆问答
- 未来信件草稿、封存与到期开启
- 好友申请、好友管理与 WebSocket 实时聊天
- MinIO 图片存储

## 技术栈

- 前端：Vue 3、Vite、Pinia、Element Plus
- 后端：Spring Boot、Spring Security、Spring Data JPA、WebSocket/STOMP
- 数据：MySQL、MinIO
- AI：DeepSeek API

## 本地开发

安装依赖并启动前端：

```bash
npm install
npm run dev
```

后端位于 `backend/`，使用 IDEA 或 Maven 启动 Spring Boot。必要环境变量参考 `.env.example`，真实密钥与密码不要提交到 Git。

## 构建

```bash
npm run build
mvn -f backend/pom.xml -DskipTests package
```
