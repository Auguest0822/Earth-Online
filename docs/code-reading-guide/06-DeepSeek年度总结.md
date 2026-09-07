# 06 DeepSeek 年度总结

## 完整链路

```text
YearlyView 选择年份
  → POST /api/ai/yearly-summary
  → YearlySummaryController
  → YearlySummaryService
  → 查询当前用户当年 records
  → 计算 sourceHash / 检查缓存
  → DeepSeekYearlySummaryClient
  → 校验结构化 JSON
  → yearly_summaries
```

## 用户和年份边界

```java
UserEntity user = requireUser(username);
List<RecordEntity> records = recordRepository
    .findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
        user.getId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
```

用户名来自 JWT，查询同时限定内部用户 ID 和年度日期范围。没有记录时返回 400，而不是让模型凭空总结。

## 缓存与重新生成

```java
String sourceHash = sourceHash(records);
Optional<YearlySummaryEntity> cached =
    summaryRepository.findByUserIdAndYear(user.getId(), year);

if (!regenerate && cached.isPresent()
        && cached.get().getSourceHash().equals(sourceHash)) {
    return toResponse(cached.get(), false);
}
```

缓存键是“用户 + 年份”。`regenerate=false` 且数据没变化时直接返回缓存，减少费用和等待；用户主动重新生成，或者源记录变化，才再次调用 AI。

## sourceHash 为什么稳定

```java
String stable = "%s\u001f%s\u001f%s\u001f%s\u001f%s\u001f%s\n".formatted(
    record.getId(), record.getRecordDate(), record.getTitle(),
    record.getContent(), record.getMood(),
    String.join("\u001e", record.getKeywords()));
digest.update(stable.getBytes(StandardCharsets.UTF_8));
```

- 记录按日期升序查询，输入顺序稳定。
- 使用不常见分隔符避免字段拼接歧义。
- 修改、增加或删除参与摘要的记录会改变 SHA-256。
- Hash 用于变化检测，不是密码哈希，也不是数据加密。

## DeepSeek 调用

```java
Map<String, Object> body = Map.of(
    "model", model,
    "messages", List.of(
        Map.of("role", "system", "content", SYSTEM_PROMPT),
        Map.of("role", "user", "content", prompt)),
    "response_format", Map.of("type", "json_object"),
    "stream", false);
```

系统提示规定角色、安全边界和 JSON 输出；用户提示包含年度记录和结构要求。`DEEPSEEK_API_KEY` 只由后端环境变量读取，不能使用 `VITE_` 变量放到浏览器。

## 限长、解析与校验

- 单条正文最多送出 800 字符；总提示最多 40000 字符，控制 Token 成本。
- `stripJsonFence` 兼容模型偶尔返回的 Markdown 围栏。
- Jackson 把内容解析为 `DeepSeekYearlySummary`。
- `isValid` 检查标题、总结、情绪、关键词、亮点和寄语是否齐全。
- 最多尝试两次；仍不完整则返回 502，不保存半成品。
- 429 映射为“AI 服务繁忙”，网络超时映射为 504。

## 为什么旧总结不会先被删除

AI 调用和校验先在内存完成，成功后才调用 `entity.update` 并保存。若 DeepSeek 超时或 JSON 解析失败，数据库中的旧缓存还在。这是外部服务失败隔离的重要设计。

## 当前实现可改进点

- `sourceHash` 当前没有包含 `eventType` 和照片变化；若这些字段影响总结，应加入稳定输入。
- 并发生成同一用户同一年总结时，可用 `(user_id, year)` 唯一约束、锁或请求合并避免重复调用。
- 当前非流式响应，不能声称实现了 SSE 或流式 AI 对话。

## 评委可能追问

**年度总结是不是 RAG？** 它使用全年的业务数据作为上下文，但任务固定、没有针对自由问题检索 Top K；项目中独立的年度回忆问答才具有明确的检索环节。

