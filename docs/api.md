# API 文档

## 基础信息
- 开发环境基址：`http://localhost:3000/api`
- 请求格式：JSON
- 认证方式：Bearer Token (JWT)

## 接口列表

### 用户模块
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |
| GET | /api/user/profile | 获取个人信息 |
| PUT | /api/user/profile | 更新个人信息 |

### 新闻模块
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/news | 获取新闻列表 |
| GET | /api/news/:id | 获取新闻详情 |

### 时间胶囊
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/capsules | 获取时间胶囊列表 |
| POST | /api/capsules | 创建时间胶囊 |
| PUT | /api/capsules/:id | 更新时间胶囊 |

### AI 模块
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/ai/mood-analysis | 心情分析 |
| POST | /api/ai/yearly-summary | 年度总结生成 |

---

> API 设计阶段。请根据实际开发进度更新。
