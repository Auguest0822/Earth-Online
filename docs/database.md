# 数据库设计

## 技术选型
- ORM：Prisma
- 数据库：开发阶段使用 SQLite，生产环境可切换 MySQL/PostgreSQL

## 数据模型（规划）

### User（用户）
```
id        Int      @id @default(autoincrement())
username  String   @unique
password  String   // 加密存储
avatar    String?  // 头像 URL
nickname  String?
bio       String?
createdAt DateTime @default(now())
```

### News（新闻）
```
id        Int      @id @default(autoincrement())
title     String
content   String
source    String?
imageUrl  String?
createdAt DateTime @default(now())
```

### TimeCapsule（时间胶囊）
```
id        Int      @id @default(autoincrement())
userId    Int
content   String
sendAt    DateTime // 定时发送时间
isSent    Boolean  @default(false)
createdAt DateTime @default(now())
```

### TimelineEvent（时间轴事件）
```
id        Int      @id @default(autoincrement())
userId    Int
title     String
content   String?
date      DateTime
createdAt DateTime @default(now())
```

### Achievement（成就）
```
id        Int      @id @default(autoincrement())
userId    Int
title     String
desc      String?
icon      String?
unlockedAt DateTime @default(now())
```

---

> 数据库设计阶段。Prisma schema 文件位于 `server/prisma/schema.prisma`。
