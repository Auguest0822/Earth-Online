# 04 生活事记 CRUD 与 JPA

## 新增事记的完整链路

```text
TimelineView 提交表单
  → records.js：POST /api/records
  → RecordController.create
  → RecordService.create
  → RecordRepository.save
  → records / record_keywords / record_photos
  → RecordResponse 返回页面
```

## DTO：先挡住非法输入

```java
public record CreateRecordRequest(
    @Size(max = 100) String title,
    @NotBlank @Size(max = 1500) String content,
    LocalDate date,
    @Size(max = 32) String eventType,
    @Size(max = 32) String mood,
    @Size(max = 5) List<@Size(max = 30) String> keywords,
    @Size(max = 9) List<@Size(max = 1000) String> photos
) { }
```

Controller 使用 `@Valid @RequestBody` 后，Spring 会先把 JSON 反序列化成 DTO，再执行 Bean Validation。DTO 与 Entity 分开，既能限制输入，也避免数据库内部字段直接暴露。

## Controller：只处理 HTTP 边界

```java
@PostMapping
public RecordResponse create(Authentication authentication,
        @Valid @RequestBody CreateRecordRequest request) {
    return recordService.create(authentication.getName(), request);
}
```

Controller 不接收 `userId`。用户名来自经过 JWT 校验的 Authentication，攻击者无法靠篡改 JSON 把记录写到别人名下。

## Service：业务规则与所有权

```java
Long userId = requireUserId(username);
RecordEntity entity = new RecordEntity(
    normalizeTitle(request.title(), request.content()),
    request.content(),
    LocalDateTime.now(),
    userId,
    request.date() == null ? LocalDate.now() : request.date(),
    normalizeEventType(request.eventType()),
    request.mood(),
    request.keywords(),
    validatePhotoObjectKeys(username, request.photos())
);
return toResponse(recordRepository.save(entity), username);
```

- 空标题从正文前 30 个字符生成。
- 用户未选日期时使用当天。
- 图片必须是该用户通过上传接口取得的对象键。
- 保存后转为 Response DTO；其中对象键转换成临时预览 URL。

更新和查询使用：

```java
recordRepository.findByIdAndUserId(id, userId)
```

这里的重点不是“先查记录，再在 Java 中比较用户”，而是直接把 `id + userId` 放进数据库条件。即使 URL 中的记录 ID 被替换，也查不到其他用户的记录。

## Repository 方法名如何变成 SQL

```java
Optional<RecordEntity> findByIdAndUserId(Long id, Long userId);
boolean existsByIdAndUserId(Long id, Long userId);
void deleteByIdAndUserId(Long id, Long userId);
```

Spring Data JPA 解析方法名，自动生成对应查询。自定义搜索使用 JPQL：

```java
select distinct record from RecordEntity record
left join record.keywords keyword
where record.userId = :userId
  and (lower(record.title) like ... or lower(keyword) like ...)
```

`left join` 展开关键词集合；`distinct` 防止一条记录因多个关键词命中而重复；`PageRequest.of(0, 20)` 限制最多返回 20 条。

## Entity 与三张表

```java
@Entity
@Table(name = "records")
public class RecordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "record_keywords",
        joinColumns = @JoinColumn(name = "record_id"))
    private List<String> keywords;
}
```

`@ElementCollection` 表示关键词不是独立业务实体，由记录拥有，JPA 使用附属表保存。照片集合同理，但保存的是 MinIO 对象键，不是图片二进制。

## 当前实现可改进点

- `findAllByUserId` 尚未真正分页，数据量大时应返回 `Page<RecordEntity>`。
- 两个 EAGER 集合在复杂查询中可能增加加载量，可按场景调整抓取策略。
- `deleteByIdAndUserId` 最好处于事务边界内，并考虑同步清理 MinIO 孤儿对象。
- 标题数据库长度 100，正文 Entity 长度 5000，而 DTO 限制正文 1500；这是接口规则比存储容量更严格，并不冲突。

## 评委可能追问

**为什么 `users.id` 和 `publicId` 都存在？** `id` 是内部主键和关联依据；`publicId` 是对外可展示、可搜索的稳定编号，避免暴露连续主键。

**为什么返回 404 而不是 403？** 对“不是自己的记录”统一返回不存在，可减少资源枚举和信息泄露。

