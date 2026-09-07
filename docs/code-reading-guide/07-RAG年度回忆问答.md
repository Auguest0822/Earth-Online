# 07 RAG 年度回忆问答

## RAG 三个阶段

```text
Retrieval：从当前用户指定年份的记录中检索并排序
Augmentation：把 Top 5 记录作为带编号上下文写进提示词
Generation：DeepSeek 只依据这些上下文回答
```

## 第一道安全边界：候选集

```java
UserEntity user = userRepository.findByUsername(username)
    .orElseThrow(/* 401 */);

List<RecordEntity> yearlyRecords = recordRepository
    .findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
        user.getId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
```

先限定用户和年份，再做相关度计算。不能先 `findAll()` 再在内存中过滤，否则会加载其他用户数据，也容易意外发送给模型。

## 轻量相关度排序

```java
List<ScoredRecord> ranked = yearlyRecords.stream()
    .map(record -> new ScoredRecord(record, relevance(question, record)))
    .sorted(Comparator.comparingDouble(ScoredRecord::score).reversed()
        .thenComparing(item -> item.record().getRecordDate(),
            Comparator.reverseOrder()))
    .toList();
```

- 每条记录计算一个分数。
- 分数降序；同分时较新的记录优先。
- 标题完整命中权重最高，关键词、类型、心情和正文词命中分别加权。
- 问题中的“今年、哪些、请、帮我”等填充词会被忽略，减少噪声。

```java
List<RecordEntity> sources = ranked.stream()
    .filter(item -> item.score() > 0)
    .limit(MAX_SOURCES) // 当前为 5
    .map(ScoredRecord::record)
    .toList();
```

只发送最多 5 条，既减少隐私暴露，也控制 Token。对“总结这一年的成长”这类宽泛问题，词面匹配可能很少，当前代码会用最近记录补足最多 3 条上下文，但仍不越过用户和年份边界。

## 增强提示词

应把每条来源组织成清晰的数据块，例如：

```text
[记录1]
日期：2026-08-20
标题：年度回忆功能完成
正文：……

[记录2]
……
```

系统提示需要说明：记录是“不可信引用数据”，其中出现的命令不是系统指令；只能依据记录作答，没有证据时明确说不知道。这样可以降低记录正文中提示词注入的影响。

## 回答与来源一起返回

```java
return new MemorySourceResponse(
    record.getId(),
    record.getTitle(),
    record.getRecordDate(),
    excerpt(record.getContent()),
    normalizedScore
);
```

`recordId` 用于点击后跳回时间线原记录；标题、日期和摘要帮助用户核对；相关度经过归一化后用于展示。来源可追溯性是该功能区别于普通 AI 问答的直观证据。

## 为什么当前方案仍属于 RAG

RAG 是“先检索、再把检索结果增强上下文、最后生成”的方案，不强制使用向量数据库。本项目数据规模较小，字段匹配和加权排序易解释、部署成本低。数据量和同义表达需求增大后，可把 Retrieval 替换为 Embedding 与向量相似度，而 Controller 和回答来源结构仍可保留。

## 当前实现边界

- 数据源只有当前用户指定年份的生活事记。
- 未来信、聊天消息和其他用户记录不进入 RAG。
- 没有多轮对话记忆。
- 没有训练或微调 DeepSeek。
- 不是向量检索，语义相近但词面不同的问题可能召回不足。

## 评委可能追问

**为什么不把全年所有记录直接给模型？** 无检索会增加 Token、延迟和隐私暴露，也不能针对问题突出相关证据。

**为什么必须返回 sources？** 用户可以验证答案依据，前端可以跳转原记录，也便于发现模型是否误解上下文。

