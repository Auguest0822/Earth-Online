# 移动端规划

## 目标
考核完成后，使用 Flutter 开发跨平台移动端（iOS + Android）。

## 技术栈（计划）
- Flutter 3.x
- Dart
- Riverpod / Provider（状态管理）
- Dio（网络请求）

## 目录位置
移动端代码放在 `apps/mobile/`，通过 Flutter CLI 创建：
```bash
cd apps
flutter create mobile
```

## 与 Web 端的关系
- 共用同一套后端 API（`server/`）
- Web 端和移动端共享业务逻辑
- 数据格式保持一致（JSON）

## 功能对照
| Web 端 | 移动端 |
|--------|--------|
| 新闻浏览 | 首页新闻流 |
| 个人主页 | 个人中心 |
| 时间胶囊 | 胶囊创建与提醒 |
| 时间轴 | 时间轴浏览 |
| 数据面板 | 简化版统计 |

---

> 这是远期规划。当前阶段专注于 Web 全栈开发。
