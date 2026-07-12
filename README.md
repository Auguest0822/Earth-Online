# Earth Online（地球在线）

一个连接“全球动态”与“个人成长记录”的 Web 全栈应用。

Earth Online 希望把分散的地球新闻、个人时间线、年度回顾和时间胶囊整理到同一个数字空间中：用户既能观察正在发生的世界，也能记录自己在地球上生活的痕迹。

> 目前处于持续开发阶段。现阶段以 Vue 3 前端和新闻聚合服务为主，用户系统、数据库持久化与 AI 功能正在规划和开发中。

## 项目状态

| 模块 | 当前状态 | 说明 |
| --- | --- | --- |
| 全球动态首页 | 已实现 | 支持新闻分类、热点事件和关键词展示 |
| 新闻聚合接口 | 已实现 | 聚合 GDELT，可选接入 NewsAPI，失败时返回演示数据 |
| 个人档案 | 前端原型 | 展示个人信息、记录统计和数据图表 |
| 个人时间线 | 前端原型 | 使用 Mock 数据展示个人事件 |
| 年度记录 | 前端原型 | 展示年度事件、心情趋势与关键词 |
| 时间胶囊 | 前端原型 | 支持创建、筛选和状态展示，暂未持久化 |
| 登录与注册 | 前端原型 | 已完成表单交互，尚未连接真实认证接口 |
| 成就系统 | 开发中 | 当前为占位页面 |
| 数据库与完整后端 | 规划中 | 计划采用 Node.js、Express、Prisma 和 SQLite/MySQL |
| AI 能力 | 规划中 | 计划用于年度总结、情绪分析和内容推荐 |
| Flutter 移动端 | 远期规划 | 将与 Web 端复用同一套后端 API |

## 核心功能

- 地球动态：浏览全球气候、科技、社会和经济等分类新闻。
- 热点聚合：通过 GDELT 与可选的 NewsAPI 获取新闻，并生成今日关键词。
- 个人记录：展示个人档案、生活统计、时间线和数据可视化。
- 年度回顾：从年度视角观察事件分布、心情趋势和关键词变化。
- 时间胶囊：给未来的自己写信，设置开启日期、心情、标签和隐私状态。
- 降级保障：外部新闻服务不可用时自动使用本地演示数据，保证页面可展示。

## 技术栈

### 当前使用

- Vue 3：前端框架
- Vite 8：开发与构建工具
- Vue Router：页面路由
- Pinia：状态管理
- Element Plus：UI 组件库
- ECharts：数据可视化
- SCSS：样式组织
- Node.js HTTP：新闻聚合接口
- GDELT / NewsAPI：新闻数据来源

### 计划接入

- Node.js + Express：业务后端
- Prisma ORM：数据库访问
- SQLite：本地开发数据库
- MySQL 或 PostgreSQL：部署阶段数据库
- JWT：用户身份认证
- 大模型 API：年度总结、情绪分析与智能推荐
- Flutter：Android移动端

## 快速开始

### 1. 环境要求

- Node.js（建议使用当前 LTS 版本）
- npm
- Git

### 2. 获取并安装项目

```bash
git clone https://github.com/Auguest0822/Earth-Online.git
cd Earth-Online
npm install
```

### 3. 启动完整开发环境

```bash
npm run dev:full
```

该命令会同时启动：

- Web 前端：`http://127.0.0.1:5173`
- 新闻接口：`http://127.0.0.1:8787`

浏览器打开 `http://127.0.0.1:5173` 即可访问项目。

### 4. 分别启动（可选）

只启动前端：

```bash
npm run dev
```

只启动新闻接口：

```bash
npm run api
```

> 只启动前端时，新闻请求无法连接本地接口，将自动使用前端内置的演示数据。

## 环境变量

项目不配置 NewsAPI Key 也可以运行，默认使用 GDELT；如需同时使用 NewsAPI，请复制环境变量模板：

```powershell
Copy-Item .env.example .env.local
```

然后修改 `.env.local`：

```env
NEWS_API_KEY=your_newsapi_key_here
NEWS_SERVER_PORT=8787
```

可选配置：

```env
VITE_NEWS_BACKEND_URL=http://127.0.0.1:8787/api
```

本地开发时通常无需填写 `VITE_NEWS_BACKEND_URL`，Vite 会把 `/api` 请求代理到新闻服务。

请勿将真实密钥提交到 GitHub。

## 常用命令

| 命令 | 作用 |
| --- | --- |
| `npm run dev` | 启动 Vite 前端开发服务器 |
| `npm run api` | 启动新闻聚合接口 |
| `npm run dev:full` | 同时启动前端与新闻接口 |
| `npm run build` | 构建生产版本 |
| `npm run preview` | 本地预览生产构建 |

## API

### 健康检查

```http
GET /api/health
```

示例地址：

```text
http://127.0.0.1:8787/api/health
```

### 获取新闻

```http
GET /api/news?category=all
```

`category` 可选值：

- `all`：全部
- `climate`：气候与生态
- `science`：科学与科技
- `society`：社会
- `economy`：经济

接口会优先返回外部新闻数据；当外部服务不可用时，自动返回演示数据。

## 项目结构

```text
Earth-Online/
├─ public/                 # 公共静态资源
├─ src/                    # Vue Web 前端
│  ├─ api/                 # 前端接口请求
│  ├─ assets/              # 图片等资源
│  ├─ components/          # 可复用组件
│  ├─ mock/                # 前端演示数据
│  ├─ router/              # Vue Router
│  ├─ stores/              # Pinia 状态管理
│  ├─ styles/              # 全局样式与变量
│  └─ views/               # 页面组件
├─ server/                 # 后端与新闻聚合服务
│  ├─ news-server.js       # 当前可运行的新闻接口
│  ├─ dev.js               # 前后端联合启动脚本
│  ├─ src/                 # Express 业务后端预留结构
│  └─ prisma/              # Prisma 数据模型
├─ apps/
│  └─ mobile/              # 未来 Flutter 移动端
├─ docs/                   # 需求、接口、数据库和开发文档
├─ .env.example            # 环境变量示例
├─ package.json
└─ vite.config.js
```

Web 前端目前保留在仓库根目录，便于考核阶段直接运行。未来的 Flutter 工程放在 `apps/mobile/` 中，两端通过 `server/` 提供的 API 共享数据和业务能力，彼此的启动命令不会冲突。

## 开发路线

1. 完成 Express 后端基础结构和统一错误处理。
2. 使用 Prisma + SQLite 建立用户、时间线、时间胶囊和成就数据模型。
3. 实现注册、登录、JWT 鉴权与用户数据隔离。
4. 将现有 Mock 数据逐步迁移到真实 API。
5. 接入大模型 API，实现年度总结与情绪分析。
6. 完善测试、部署说明和暑期考核报告。
7. Web 端稳定后，在 `apps/mobile/` 创建 Flutter 应用并复用后端接口。

更详细的规划见 [docs](./docs/) 目录。

## 项目说明

- 本项目仍在开发中，部分页面和交互使用 Mock 数据。
- `node_modules/`、`dist/`、`.env` 和 `.env.local` 不应提交到 Git。
- 外部新闻内容及图片版权归原作者或来源平台所有，本项目仅用于学习与课程考核展示。

## 作者

[Auguest0822](https://github.com/Auguest0822)

欢迎通过 Issue 提出建议。
