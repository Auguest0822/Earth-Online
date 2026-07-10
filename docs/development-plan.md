# 开发计划

## 项目背景
大学暑期考核项目，30 天内完成。原为前端方向，现拓展为全栈 + AI 应用。

## 技术栈

### 前端
- Vue 3（Composition API）
- Vite 8（构建工具）
- Element Plus（UI 组件库）
- ECharts 6（数据可视化）
- Pinia（状态管理）
- Vue Router（路由）
- SCSS（样式）

### 后端（计划）
- Node.js + Express
- Prisma ORM
- MySQL / SQLite（开发阶段可用 SQLite）
- JWT 认证

### AI 集成（计划）
- OpenAI API / 国内大模型 API
- 应用场景：心情分析、年度总结生成、智能推荐

## 30 天开发路线

### 第 1 周：基础搭建
- [ ] 完成目录结构整理
- [ ] 搭建 Express 后端骨架
- [ ] 配置 Prisma + 数据库
- [ ] 实现用户注册/登录 API

### 第 2 周：核心功能后端化
- [ ] 将前端 mock 数据迁移到后端 API
- [ ] 新闻 API 开发
- [ ] 时间胶囊后端逻辑

### 第 3 周：AI 功能
- [ ] 接入 AI API
- [ ] 实现心情分析
- [ ] 实现年度总结自动生成

### 第 4 周：完善与部署
- [ ] 前端联调
- [ ] 错误处理与优化
- [ ] 撰写考核报告
- [ ] 部署上线（可选）

## 每日开发规范
1. 开始前 `git pull` 同步最新代码
2. 每个功能完成后 `git commit`，不要积攒
3. 提交信息使用约定式提交格式（feat/fix/docs/chore）
4. 每天结束前 `git push` 到 GitHub

---

> 最后更新：2026-07-10
