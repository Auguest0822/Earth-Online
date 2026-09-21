# Earth Online

把个人生活轨迹沉淀成时间线与年度回顾的全栈项目：日常记录、心情与图片，逐年回看；再往前延伸出「给未来的自己写信」和好友实时聊天。

AI 部分使用 DeepSeek：一份结构化年度总结，以及一个**只依据本人当年记录作答、并附上引用来源**的年度回忆问答。

> 前端 Vue 3 + Vite　后端 Spring Boot + JPA　数据 MySQL + MinIO　AI DeepSeek

---

## 功能

**账号与资料**
- 注册登录、令牌鉴权、路由守卫
- 个人资料、头像上传（图片走对象存储）

**记录与回顾**
- 生活事记的增删改查：标题、正文、日期、事件类型、心情、关键词标签、图片
- 时间线浏览与关键词 / 类型搜索
- 年度视图：记录数量、事件分布、关键词统计

**AI 年度回忆**
- 年度总结：按固定结构生成标题、总述、心情概况、关键词洞察、亮点与一封信
- 问问这一年：轻量 RAG（先检索本人该年记录，再交给模型作答），回答带引用来源，可跳回原记录
- 问答与总结都有缓存：同一用户同一年只保留一份，记录变化时自动提示"可以重新提问"
- 回答支持流式输出（SSE），文字逐字出现

**未来信件**
- 写信、封存、到期开启；未到期的信件只显示倒计时

**好友与聊天**
- 通过 10 位地球 ID 搜索并添加好友
- 基于 WebSocket / STOMP 的实时聊天：即时收消息、未读提醒、已读回执、图片与表情

---

## 技术栈

| 层 | 选型 |
| --- | --- |
| 前端 | Vue 3（组合式 API）、Vite、Pinia、Vue Router、Element Plus、ECharts |
| 后端 | Java 21、Spring Boot、Spring Security、Spring Data JPA、WebSocket / STOMP |
| 数据 | MySQL 8（业务数据）、MinIO（图片对象存储） |
| AI | DeepSeek 对话接口（年度总结、年度回忆问答） |
| 工程 | npm workspaces、Maven、Docker |

---

## 项目结构

```text
earth-online/
├─ web/               前端：页面、组件、状态、接口封装
│  └─ src/{views,components,stores,api,router,styles}
├─ backend/           后端：接口、业务、数据访问、AI 调用
│  └─ src/main/java/.../backend/{auth,user,records,friends,chat,futureletters,ai,storage,config,common}
├─ deploy/            本机容器化运行所需的编排文件、后端镜像与环境变量模板
├─ docs/              开发笔记、学习指南、排障记录与答辩材料（仅本地，不随仓库发布）
├─ output/            手工验证脚本（WebSocket、图片上传）
└─ .env.example       环境变量模板（复制后填写，不要提交真实密钥）
```

> `server/` 是早期尝试的 Node 后端骨架，已被 Spring Boot 后端取代，可忽略。

---

## 快速开始

### 0. 环境要求

- Node.js 18+
- JDK 21+
- MySQL 8（本机安装或用 `deploy/docker-compose.yml` 起一个容器）
- MinIO（可选，本机安装或用同一份编排文件启动；不上传图片时可以先不启）

### 1. 准备数据库

```sql
CREATE DATABASE earth_online CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**不需要手工建表**：后端启动时会按数据模型的声明自动创建表结构。

### 2. 启动后端

```bash
cd backend
./mvnw spring-boot:run        # Windows 用 mvnw.cmd
```

配置通过**环境变量**注入（在 IDE 的运行配置里填写，或在终端 export），参考 `.env.example`：数据库连接、令牌密钥、MinIO 密钥、DeepSeek 密钥。
真实密钥不要写进代码，也不要提交到 Git。

### 3. 启动前端

```bash
npm install
npm run dev
```

打开 `http://localhost:5173`。开发服务器会把 `/api` 与 `/ws` 请求代理到本机后端（默认 `localhost:8080`），
所以本地开发不需要额外配置跨域。

> 想验证好友与聊天，需要两个账号：用**隐身窗口**登录第二个账号（普通标签页会共用同一份登录凭证）。

### 4. 打包构建

```bash
npm run build                    # 产出 web/dist
cd backend && ./mvnw -DskipTests package   # 产出可执行 jar
```

---

## 配置项

| 变量 | 作用 |
| --- | --- |
| `VITE_API_BASE_URL` | 前端请求接口的地址，默认 `/api`（相对路径） |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 数据库连接 |
| `JWT_SECRET` | 签发登录令牌用的密钥，至少 32 位随机字符串 |
| `JWT_EXPIRATION_MS` | 令牌有效期，默认 1 天 |
| `MINIO_ENDPOINT` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 图片对象存储 |
| `DEEPSEEK_API_KEY` | AI 调用密钥（**只能放在后端**） |
| `DEEPSEEK_BASE_URL` / `DEEPSEEK_MODEL` | AI 服务地址与模型名 |

---

## 文档

项目的开发笔记、学习指南与答辩材料（功能说明、排障记录、AI 回忆与流式输出实现笔记、答辩自查问答手册等）都放在本机的 `docs/` 目录里，**不随仓库发布**（已在 `.gitignore` 中忽略）。

---

## 已知局限

- 年度回忆的检索是**字段加权匹配**（标题、关键词、类型、心情、正文），不理解同义表达；数据量变大时可升级为向量检索与混合排序。
- AI 回答依赖外部服务，服务不可用时接口会返回明确错误提示，不会写入缓存。
- 项目目前面向单机小规模运行，未做限流、用量统计与水平扩容。
