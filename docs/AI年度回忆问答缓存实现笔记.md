# AI 年度回忆问答缓存实现笔记

> 记录日期：2026-09-18
> 涉及功能：年度记录页 → AI 年度回忆 →「问问这一年」
> 相关文件：`backend/.../ai/YearlyMemory*.java`、`backend/.../ai/YearlyRecordFingerprint.java`、`web/src/views/YearlyView.vue`、`web/src/api/modules/stats.js`

这份文档回答三件事：**做了什么**、**为什么这么做**、**怎么验证和上线**。写法与年度总结缓存（`yearly_summaries`）保持一致，两个功能现在是同一套缓存思路。

---

## 1. 需求与效果

**需求**：在「问问这一年」里提问一次之后，下次再进入年度记录页，应该直接看到上一次的问答内容，而不是重新让 AI 再答一遍。

**效果**：

| 场景 | 缓存前 | 缓存后 |
| --- | --- | --- |
| 再次进入页面 | 空白，需要重新提问 | 自动回填上次的问题 + 回答 + 回答依据 |
| 是否调用 AI | 每次提问都调用 | 只有提问时调用一次，重进页面只读数据库 |
| 耗时 | 约 1.3～4 秒（取决于 AI） | 约 120 毫秒（实测线上 121ms vs 1304ms） |
| 记录变化后 | 无感知 | 回答下方出现提醒：「这一年的记录在回答之后有变化，可以重新提问获得新的回答。」 |

页面右上角还会显示「上次提问于 2026年9月18日 10:15」，让用户知道这条回答不是刚刚生成的。

---

## 2. 数据表

新表 `yearly_memory_answers`，**一个用户一年只保留一条**（与 `yearly_summaries` 一样的粒度）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint 主键自增 | |
| `user_id` | bigint | 归属用户 |
| `summary_year` | int | 归属年份 |
| `question` | varchar(500) | 上一次的提问 |
| `answer` | text | 上一次的 AI 回答 |
| `sources_json` | text | 回答依据（引用到的记录）整段存 JSON，读出来即可还原卡片 |
| `source_hash` | varchar(64) | 生成这条回答时「这一年记录」的内容指纹 |
| `model` | varchar(64) | 当时使用的模型名 |
| `prompt_version` | varchar(32) | 当时使用的提示词版本（当前 `v1`） |
| `created_at` / `updated_at` | datetime | 创建时间 / 最后更新时间 |

唯一约束：`uk_yearly_memory_user_year (user_id, summary_year)`。

**不需要手工建表**：后端启动时 JPA `ddl-auto=update` 会自动补齐（和生产环境一样，本次上线就是这样建出来的）。

---

## 3. 两条流程

### 3.1 提问（POST `/api/ai/yearly-memory/query`）

```text
校验登录 → 取出这一年的记录（空则 400）
  → 词法打分排序（原有 RAG 逻辑，最多 5 条依据，不足 3 条时用最近的记录补足）
  → 调用 DeepSeek 生成回答
  → 计算这一年记录的 SHA-256 指纹
  → 覆盖写入 yearly_memory_answers（没有行就新建）
  → 返回回答 + 依据 + stale=false + updatedAt
```

### 3.2 再次进入页面（GET `/api/ai/yearly-memory/answer?year=2025`）

```text
校验登录 → 取出这一年的记录 → 算指纹
  → 查缓存：没有 → 204 No Content（前端显示空状态）
             有   → 200，返回存的问答
                    stale = (缓存里的指纹 != 现在的指纹)
```

`204` 这个约定和年度总结的 GET 接口完全一致，前端 `request()` 遇到 204 会拿到空字符串，正好当成「没有缓存」。

---

## 4. 代码位置一览

| 文件 | 作用 |
| --- | --- |
| `ai/YearlyMemoryAnswerEntity.java`（新增） | 缓存表实体 |
| `ai/YearlyMemoryAnswerRepository.java`（新增） | `findByUserIdAndYear` |
| `ai/YearlyRecordFingerprint.java`（新增） | 年度记录指纹工具（年度总结与问答共用） |
| `ai/YearlyMemoryService.java` | `query()` 落库、`getSaved()` 读缓存、`toResponse()` 还原 |
| `ai/YearlyMemoryController.java` | 新增 `GET /answer`（`@Validated` + `@Min/@Max` 校验年份） |
| `ai/dto/YearlyMemoryResponse.java` | 增加 `stale`、`updatedAt` |
| `ai/YearlyMemoryAiClient.java` | 增加 `model()`，把模型名一起存进缓存 |
| `ai/YearlySummaryService.java` | 改为复用 `YearlyRecordFingerprint`，删掉重复的私有指纹方法 |
| `web/src/api/modules/stats.js` | 新增 `getYearlyMemoryAnswer(year)` |
| `web/src/views/YearlyView.vue` | `loadMemoryAnswer()` + 挂载/切年份时读取 + 时间与过期提醒 |

---

## 5. 关键设计点

### 5.1 为什么是「一用户一年一条、每次覆盖」

需求里说的是「看到**上一次**的问答内容」，所以不需要保存历史问答列表。覆盖式写入让表结构最简单，查询也永远是「按用户 + 年份取一条」，不用排序、不用分页。

### 5.2 指纹（sourceHash）解决「缓存什么时候该失效」

指纹覆盖所有会进入提示词的字段：记录 id、日期、标题、正文、心情、关键词，字段之间用不可见字符 `\u001f`/`\u001e` 分隔，再对整串做 SHA-256。

- 记录**新增、修改、删除** → 指纹变化 → `stale = true`；
- 记录**没动** → 指纹一致 → `stale = false`。

用不可见字符做分隔符是为了避免「正文里正好出现分隔符」导致不同内容算出同一指纹。

### 5.3 一个指纹工具，两个功能共用

年度总结原本自己写了一份指纹计算。这次抽出 `YearlyRecordFingerprint`，两个功能都调用它。好处是**口径永远一致**：同一年记录变化时，总结和问答的过期提醒会同时出现，不会出现「总结说变了、问答说没变」的尴尬。因为格式一模一样，历史缓存行的指纹仍然有效。

### 5.4 `model` 和 `prompt_version` 是给未来留的口子

如果以后换了模型或改了提示词，靠这两列就能判断「旧缓存要不要重算」，不用靠时间去猜。

### 5.5 前端行为

```text
进入页面 / 切换年份 → loadMemoryAnswer()
  ├─ 204（无缓存）→ 清空回答区，输入框保留用户自己输入的问题
  └─ 200（有缓存）→ 回填问题 + 回答 + 依据卡片；显示「上次提问于 …」
                    stale=true 时多显示一条黄色提醒
```

读缓存失败时只在控制台 `console.warn`，不弹错误提示——因为即使缓存读不到，用户直接点「开始回忆」仍然可用。

---

## 6. 踩过的坑：Jackson 2 不认识 `LocalDate`

第一次自测时提问直接返回 `500 回忆问答保存失败`，日志里的根因是：

```text
InvalidDefinitionException: Java 8 date/time type `java.time.LocalDate` not supported by default:
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
```

原因：这个项目同时存在两套 Jackson。

| 层 | Jackson 版本 | 用途 |
| --- | --- | --- |
| Web 层（Controller 返回 JSON） | Jackson 3（`tools.jackson`） | Spring Boot 4 默认，天然支持 `java.time` |
| AI 相关（`JacksonCompatibilityConfig` 注入） | Jackson 2（`com.fasterxml.jackson.databind`） | 给手写的 RestClient 解析 DeepSeek 响应 |

而项目里**没有引入 Jackson 2 的 jsr310 模块**，所以用注入的 `ObjectMapper` 序列化带 `LocalDate` 的 `MemorySourceResponse` 就会失败。

**处理方式**（不新增依赖）：在服务里单独定义一个用于落库的 `SavedSource` record，日期存 ISO 字符串，读出来再 `LocalDate.parse()` 还原：

```java
private record SavedSource(Long recordId, String title, String date, String excerpt, double score) {}
```

好处是不用动依赖、存进库的 JSON 也可读（`"date":"2025-03-12"`）；代价是多了一次字符串转换。

> 记忆点：**在这个项目里，凡是交给 AI 那套 Jackson 2 去序列化的对象，都不要直接带 `java.time` 类型。**

---

## 7. 接口速查

```http
POST /api/ai/yearly-memory/query
{ "year": 2025, "question": "这一年有哪些让我开心的事情？" }
→ 200 { question, answer, year, sources:[{recordId,title,date,excerpt,score}], stale:false, updatedAt }

GET /api/ai/yearly-memory/answer?year=2025
→ 200（有缓存，同上结构） / 204（无缓存） / 400（year 不在 2000-2100） / 401（未登录）
```

---

## 8. 验证记录（2026-09-18）

| 阶段 | 方式 | 结果 |
| --- | --- | --- |
| 本地接口 | 新 jar 连线上数据库（SSH 隧道）跑 30 项断言：注册登录、建记录、生成总结、首次 204、提问落库、再次读取、stale 翻转、覆盖更新、参数与鉴权边界 | 30/30 通过 |
| 本地界面 | puppeteer 驱动真实 Chrome 打开 `localhost:5173/yearly`：切到 2025 年，检查回答、问题回填、「上次提问于」、依据卡片、过期提醒、无 JS 报错 | 12/12 通过 |
| 线上接口 | 部署后对 `http://190.92.245.191` 跑 14 项断言（含真实 AI 调用，缓存 121ms vs AI 1304ms） | 14/14 通过 |
| 线上界面 | 用临时账号打开线上页面截图确认 | 7/7 通过 |

测试用的临时账号（本地 2 个、线上 1 个）及其记录、总结、问答缓存**全部已删除**，正式数据未受影响。

回归验证：年度总结的生成、读缓存、过期标记同时通过——说明抽取指纹工具没有改坏原功能。

---

## 9. 上线记录（华为云）

```text
本地：mvnw -DskipTests package　→　npm run build
上传：jar → /tmp/backend-new.jar；dist → /tmp/earth-online-dist；docker-compose.yml → /tmp
服务器：备份数据库（~/earth_online_before_memory_cache.sql，9 张表）
      → docker compose --env-file ../.env up -d --build --force-recreate backend
      → 替换 /var/www/earth-online 静态文件
      → curl 健康检查 200、新接口未登录 401、SHOW TABLES 确认新表
```

Nginx 配置无需改动（`client_max_body_size`、`/ws/` Upgrade、`/index.html` 不缓存这些片段线上已存在）。
本地还顺手重新上传了一次 `deploy/docker-compose.yml`：服务器上那份的中文注释此前被写坏成了乱码，本次替换为仓库里的正常版本。

---

## 10. 答辩可能被追问的问题

**Q：缓存会不会一直是旧的？**
不会。只要这一年的记录有增删改，指纹就变，`stale` 立刻变成 `true`，页面出现「记录有变化，可以重新提问」的提醒；用户点一下「开始回忆」就会覆盖成新回答。

**Q：为什么不用 Redis？**
缓存内容是「一个用户一年一条」的结构化数据，量很小、要求持久化、还要和记录变化联动判断过期，放在同一个 MySQL 里最简单，也省了多引一个中间件的运维成本。真要做高并发再抽出来也不迟。

**Q：如果两次提问之间用户什么都没改，会重复调用 AI 吗？**
会（这是用户主动点击的），但**再次进入页面不会**——页面的默认行为是读缓存，只有点击「开始回忆」才会调用 AI。

**Q：缓存里存了 AI 回答，会不会泄露别人的数据？**
不会。查询永远带 `user_id` 条件（`findByUserIdAndYear`），接口层再从登录令牌取用户名，用户只能读到自己的缓存。
