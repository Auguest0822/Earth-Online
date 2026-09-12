# Spring Boot、MySQL、MinIO 从零到 Earth Online 项目答辩

> 适合对象：没有系统学习过 Spring Boot、MySQL、MinIO，希望看懂当前项目后端并完成答辩。  
> 学习方式：概念 → 项目源码 → 请求链路 → 做题 → 参考答案 → 答辩表达。  
> 项目后端：Java 21、Spring Boot、Spring MVC、Spring Security、Spring Data JPA、MySQL、MinIO、WebSocket/STOMP。

---

## 0. 学习目标与学习路线

完成本文后，你应该能够：

1. 解释 Spring Boot 解决了什么问题。
2. 看懂 Controller、Service、Repository、Entity 和 DTO。
3. 追踪一个 HTTP 请求如何进入后端、访问数据库并返回 JSON。
4. 看懂常见 Spring 注解和依赖注入。
5. 理解 MySQL 的表、主键、唯一约束、索引、外键思想和事务。
6. 看懂 JPA 如何把 Java Entity 映射成 MySQL 表。
7. 理解 JWT 如何经过过滤器变成 `Authentication`。
8. 理解为什么业务查询必须同时限制资源 ID 和当前用户 ID。
9. 解释 MinIO 的 Bucket、Object Key 和预签名 URL。
10. 完整说明图片为什么存 MinIO，而不是直接存进 MySQL。
11. 回答答辩中常见的架构、安全、事务和存储问题。

推荐学习顺序：

| 阶段 | 内容 | 建议时间 |
|---|---|---:|
| 第一阶段 | Java 基础、Spring Boot、分层架构 | 2～3 天 |
| 第二阶段 | REST、DTO、校验、异常处理 | 2 天 |
| 第三阶段 | MySQL、JPA、Repository、事务 | 3～4 天 |
| 第四阶段 | Security、JWT、数据权限 | 2～3 天 |
| 第五阶段 | MinIO、上传链路、预签名 URL | 2 天 |
| 第六阶段 | 综合链路和答辩模拟 | 2～3 天 |

源码推荐阅读顺序：

1. `backend/pom.xml`
2. `backend/src/main/resources/application.yaml`
3. `BackendApplication.java`
4. `RecordController.java`
5. `RecordService.java`
6. `RecordRepository.java`
7. `RecordEntity.java`
8. `CreateRecordRequest.java`、`RecordResponse.java`
9. `SecurityConfig.java`
10. `JwtAuthenticationFilter.java`、`JwtService.java`
11. `UploadController.java`
12. `StorageService.java`、`MinioConfig.java`

---

# 第一章：Spring Boot 是什么

## 1.1 Java、Spring 和 Spring Boot 的关系

- Java：编程语言和运行平台。
- Spring Framework：提供依赖注入、Web、事务、数据库访问等能力。
- Spring Boot：基于 Spring 提供自动配置、Starter、内嵌服务器和统一配置，让项目快速运行。

可以这样理解：

```text
Java            = 建筑材料
Spring          = 建筑框架和施工方法
Spring Boot     = 带默认方案的整套施工系统
```

Spring Boot 并没有替代 Spring，而是让 Spring 项目更容易配置和启动。

## 1.2 项目启动入口

文件：`backend/src/main/java/com/earthonline/backend/BackendApplication.java`

```java
@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
```

`@SpringBootApplication` 可以先理解为三个能力的组合：

1. 声明这是 Spring Boot 配置入口；
2. 开启自动配置；
3. 从当前包及子包扫描 Spring 组件。

主类位于 `com.earthonline.backend`，因此它能扫描：

```text
com.earthonline.backend.records
com.earthonline.backend.user
com.earthonline.backend.storage
com.earthonline.backend.config
...
```

如果组件被放到主包之外，又没有额外配置扫描路径，Spring 可能发现不了它。

## 1.3 内嵌 Web 服务器

运行主方法后，Spring Boot 会：

```text
读取配置
  ↓
创建 Spring 容器
  ↓
扫描并创建 Bean
  ↓
配置数据库、Security、Web MVC
  ↓
启动内嵌 Web 服务器
  ↓
监听 8080 端口
```

项目配置：

```yaml
server:
  port: 8080
```

因此开发时后端通常访问 `http://localhost:8080`。

## 1.4 Starter 依赖

`backend/pom.xml` 使用 Maven 管理依赖。关键 Starter：

| 依赖 | 作用 |
|---|---|
| `spring-boot-starter-webmvc` | REST API、Controller、JSON Web 服务 |
| `spring-boot-starter-data-jpa` | JPA、Hibernate、Repository |
| `spring-boot-starter-security` | 认证、授权和过滤器链 |
| `spring-boot-starter-validation` | DTO 参数校验 |
| `spring-boot-starter-websocket` | WebSocket/STOMP |
| `mysql-connector-j` | Java 与 MySQL 通信的 JDBC 驱动 |
| `minio` | 调用 MinIO 对象存储 API |
| `jjwt-*` | 生成、解析和验证 JWT |

Starter 的意义是把一组常用依赖和自动配置组合起来，减少手工配置。

## 1.5 Maven 基础

项目常用命令：

```bash
mvn -f backend/pom.xml spring-boot:run
mvn -f backend/pom.xml test
mvn -f backend/pom.xml -DskipTests package
```

- `compile`：编译源码；
- `test`：运行测试；
- `package`：打包；
- `spring-boot:run`：运行应用。

## 1.6 本章自测

1. Spring Boot 和 Spring Framework 是什么关系？
2. `@SpringBootApplication` 的三个核心作用是什么？
3. 项目为什么可以直接运行 main 方法启动 Web 服务？
4. MySQL Connector 和 Spring Data JPA 分别负责什么？

<details>
<summary>查看参考答案</summary>

1. Spring Boot 基于 Spring，通过自动配置和 Starter 降低 Spring 应用的配置成本。
2. 配置入口、自动配置、组件扫描。
3. Spring Boot Web Starter 提供内嵌 Web 服务器和自动配置。
4. Connector 是 JDBC 驱动，负责底层连接 MySQL；Spring Data JPA 提供面向 Entity/Repository 的数据访问抽象。

</details>

## 1.7 答辩表达

> 本项目使用 Spring Boot 构建后端。它通过 Starter 和自动配置整合 Spring MVC、Spring Security、Spring Data JPA 与 WebSocket，并使用内嵌 Web 服务器运行。主启动类位于根包，Spring 会扫描各业务子包中的 Controller、Service、Repository 和配置类。

---

# 第二章：IoC、Bean 与依赖注入

## 2.1 什么是 Bean

Bean 是由 Spring 容器创建和管理的对象。

常见 Bean 来源：

```java
@RestController
@Service
@Repository
@Component
@Configuration
```

还可以在配置类中使用：

```java
@Bean
public MinioClient minioClient(...) {
    return MinioClient.builder().build();
}
```

## 2.2 什么是 IoC

IoC 是控制反转。普通代码由业务类自己创建依赖：

```java
RecordRepository repository = new RecordRepository(...);
```

Spring 的方式是把创建和组装对象的责任交给容器：

```java
public RecordService(
        RecordRepository recordRepository,
        UserRepository userRepository,
        StorageService storageService
) {
    this.recordRepository = recordRepository;
    this.userRepository = userRepository;
    this.storageService = storageService;
}
```

Spring 发现 `RecordService` 需要三个依赖，就从容器中找到对应 Bean 注入。

## 2.3 为什么项目使用构造器注入

优点包括：

- 依赖清楚；
- 字段可声明为 `final`；
- 创建对象时必须提供完整依赖；
- 更容易编写单元测试；
- 减少隐藏依赖。

项目没有大量使用字段上的 `@Autowired`，是较好的实践。

## 2.4 `@Configuration` 和 `@Bean`

MinIO SDK 的 `MinioClient` 不是项目自己编写的类，不能给它增加 `@Component`。因此项目在 `MinioConfig` 中创建：

```java
@Configuration
public class MinioConfig {
    @Bean
    public MinioClient minioClient(...) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

此后 `StorageService` 可以通过构造器注入 `MinioClient`。

## 2.5 依赖注入题

1. `RecordService` 是否应该在方法中反复 `new RecordRepository()`？
2. 第三方类无法添加 `@Component` 时，怎样交给 Spring 管理？
3. 构造器注入为什么比隐藏的全局对象更容易测试？

<details>
<summary>查看参考答案</summary>

1. 不应该。Repository 由 Spring Data 创建并注入。
2. 在 `@Configuration` 类中通过 `@Bean` 方法创建。
3. 测试时可以明确传入假的 Repository 或 Mock，依赖关系也更清晰。

</details>

---

# 第三章：项目分层架构

## 3.1 四个主要层次

```text
浏览器 HTTP 请求
      ↓
Controller：接收参数、映射 URL、取得当前用户
      ↓
Service：业务规则、权限检查、事务编排
      ↓
Repository：查询和保存 Entity
      ↓
MySQL
```

DTO 位于边界上：

```text
请求 JSON → Request DTO → Service
Entity → Response DTO → 响应 JSON
```

## 3.2 Controller 的职责

`RecordController`：

```java
@RestController
@RequestMapping("/api/records")
public class RecordController {
    @PostMapping
    public RecordResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateRecordRequest request
    ) {
        return recordService.create(
                authentication.getName(),
                request
        );
    }
}
```

它主要负责：

- 把 URL 和 Java 方法绑定；
- 把 JSON 转成 DTO；
- 触发校验；
- 获取当前认证用户；
- 调用 Service；
- 返回响应对象。

Controller 不应堆积复杂业务规则和数据库细节。

## 3.3 Service 的职责

`RecordService.create` 会：

1. 根据用户名获取用户 ID；
2. 处理标题和默认日期；
3. 校验图片 Object Key；
4. 创建 Entity；
5. 保存数据库；
6. 转换为响应 DTO。

这些属于业务逻辑，所以放在 Service。

## 3.4 Repository 的职责

```java
public interface RecordRepository
        extends JpaRepository<RecordEntity, Long> {
    List<RecordEntity> findAllByUserId(Long userId);
    Optional<RecordEntity> findByIdAndUserId(Long id, Long userId);
}
```

Repository 描述数据访问意图。Spring Data JPA 会为符合命名规则的方法生成实现。

## 3.5 Entity 的职责

`RecordEntity` 描述 Java 对象与数据库表的映射，包含：

- 表名；
- 主键；
- 字段列；
- 长度和可空约束；
- 集合映射。

Entity 主要用于持久化，不应直接作为所有 API 的输入输出。

## 3.6 分层题

把下列逻辑放到合适层次：

1. 映射 `POST /api/records`。
2. 检查图片是否属于当前用户目录。
3. 执行 `findByIdAndUserId`。
4. 定义 `content` 最大长度。
5. 返回给前端的图片预览 URL。

<details>
<summary>查看参考答案</summary>

1. Controller。
2. Service。
3. Repository。
4. DTO 校验和 Entity 数据库约束都需要。
5. Response DTO 的构建过程通常在 Service 中完成。

</details>

## 3.7 答辩表达

> 项目采用 Controller、Service、Repository 分层。Controller 处理 HTTP 协议和参数，Service 集中业务规则与资源权限，Repository 使用 Spring Data JPA 访问 MySQL，Entity 负责表映射，DTO 隔离接口模型和持久化模型。这样可以降低耦合，也便于测试和维护。

---

# 第四章：Spring MVC 与 REST API

## 4.1 REST 的资源思想

项目把生活记录看作资源：

| 操作 | HTTP 方法 | 地址 |
|---|---|---|
| 查询列表 | GET | `/api/records` |
| 查询单条 | GET | `/api/records/{id}` |
| 创建 | POST | `/api/records` |
| 部分更新 | PATCH | `/api/records/{id}` |
| 删除 | DELETE | `/api/records/{id}` |
| 搜索 | GET | `/api/records/search?query=学习` |

HTTP 方法表达操作语义，URL 主要表达资源。

## 4.2 常见注解

| 注解 | 作用 |
|---|---|
| `@RestController` | Controller 返回值写入响应体，通常转为 JSON |
| `@RequestMapping` | 类或方法的公共路径 |
| `@GetMapping` | 处理 GET |
| `@PostMapping` | 处理 POST |
| `@PatchMapping` | 处理 PATCH |
| `@DeleteMapping` | 处理 DELETE |
| `@RequestBody` | 从 JSON 请求体读取对象 |
| `@RequestParam` | 读取查询参数或表单参数 |
| `@PathVariable` | 读取 URL 路径变量 |
| `Authentication` | 读取当前登录用户 |

例子：

```java
@GetMapping("/{id}")
public RecordResponse getById(
        Authentication authentication,
        @PathVariable Long id
) { ... }
```

请求 `/api/records/12` 时，`id=12`。

## 4.3 JSON 转换

前端发送：

```json
{
  "title": "今天",
  "content": "学习了 Spring Boot",
  "date": "2026-09-01",
  "eventType": "学习成长",
  "mood": "😊",
  "keywords": ["Spring", "Java"],
  "photos": []
}
```

Jackson 将它转换成 `CreateRecordRequest`。返回 `RecordResponse` 时，Jackson 再将 Java 对象序列化为 JSON。

## 4.4 状态码

项目常见状态：

| 状态码 | 含义 | 项目场景 |
|---:|---|---|
| 200 | 成功 | 查询、更新 |
| 400 | 请求错误 | 参数校验失败、图片类型错误 |
| 401 | 未认证 | JWT 缺失、无效或过期 |
| 403 | 无权限 | 不是好友却发送消息 |
| 404 | 不存在 | 当前用户找不到指定记录 |
| 409 | 状态冲突 | 用户名重复、好友请求已处理 |
| 500 | 服务器内部错误 | 未预期异常 |
| 503 | 服务暂不可用 | 外部依赖不可用的场景 |

## 4.5 REST 题

1. `/api/records/5` 中的 5 应使用什么注解读取？
2. `/api/records/search?query=旅行` 中 query 使用什么注解？
3. 创建记录的 JSON 使用什么注解接收？
4. 用户未登录和已经登录但无权限分别更适合什么状态码？

<details>
<summary>查看参考答案</summary>

1. `@PathVariable`。
2. `@RequestParam`。
3. `@RequestBody`。
4. 401 和 403。

</details>

---

# 第五章：DTO、Entity 与参数校验

## 5.1 为什么不直接用 Entity 接收请求

如果前端可以直接提交 `RecordEntity`，可能尝试修改：

- 数据库主键；
- `userId`；
- `createdAt`；
- 其他不应开放的字段。

Request DTO 只暴露允许客户端填写的字段。

项目使用 Java Record：

```java
public record CreateRecordRequest(
        String title,
        String content,
        LocalDate date,
        String eventType,
        String mood,
        List<String> keywords,
        List<String> photos
) {}
```

Java Record 很适合不可变的数据传输对象。

## 5.2 Bean Validation

```java
@NotBlank(message = "内容不能为空")
@Size(max = 1500, message = "事记内容不能超过 1500 字")
String content
```

集合和集合元素都能校验：

```java
@Size(max = 5, message = "最多 5 个关键词")
List<@Size(max = 30) String> keywords
```

Controller 使用 `@Valid` 才会触发 DTO 校验：

```java
@Valid @RequestBody CreateRecordRequest request
```

## 5.3 DTO 默认值处理

紧凑构造器：

```java
public CreateRecordRequest {
    keywords = keywords == null
            ? List.of()
            : List.copyOf(keywords);
    photos = photos == null
            ? List.of()
            : List.copyOf(photos);
}
```

这样 Service 不需要反复判断集合是否为 null。

## 5.4 DTO 校验与数据库约束都要有

DTO：

```java
@Size(max = 100)
String title
```

Entity：

```java
@Column(nullable = false, length = 100)
private String title;
```

两者职责不同：

- DTO 校验尽早给调用方清晰错误；
- 数据库约束保护最终数据完整性。

只依靠前端 maxlength 不安全，因为请求可以绕过浏览器页面直接发送。

## 5.5 校验题

1. `@NotNull` 与 `@NotBlank` 对字符串有什么差异？
2. 为什么 Controller 上要写 `@Valid`？
3. 前端已有 1500 字限制，后端还需要限制吗？

<details>
<summary>查看参考答案</summary>

1. `@NotNull` 只拒绝 null；`@NotBlank` 还拒绝空字符串和只含空白的字符串。
2. 用于触发嵌套 DTO 的 Bean Validation。
3. 需要。客户端不可信，后端必须独立校验。

</details>

---

# 第六章：统一异常处理

## 6.1 业务异常

Service 中可以抛出：

```java
throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "记录不存在"
);
```

这使业务方法不需要层层构造错误响应。

## 6.2 全局异常处理器

文件：`common/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(...) {
        ...
    }
}
```

它把不同异常转换为统一 JSON：

```json
{
  "status": 404,
  "code": "REQUEST_FAILED",
  "message": "记录不存在",
  "timestamp": "2026-09-01T10:00:00"
}
```

优点：

- Controller 更简洁；
- 前端获得稳定错误结构；
- 避免把 Java 堆栈暴露给用户；
- 统一状态码和提示。

## 6.3 未预期异常

```java
@ExceptionHandler(Exception.class)
```

兜底返回 500 和通用消息。真正的异常详情应该记录在服务端日志，而不是直接返回给浏览器。

## 6.4 异常题

1. 用户请求不存在的记录，应该返回 404 还是 500？
2. 为什么不能把完整堆栈返回给前端？
3. 参数校验异常在项目中被转换成什么状态码？

<details>
<summary>查看参考答案</summary>

1. 404，这是预期业务错误。
2. 堆栈可能泄漏类名、路径、SQL 和内部结构，也不适合终端用户阅读。
3. 400。

</details>

---

# 第七章：MySQL 基础

## 7.1 数据库、表、行和列

项目默认数据库：

```text
earth_online
```

可能包含：

```text
users
records
record_keywords
record_photos
future_letters
friend_requests
chat_messages
yearly_summaries
```

以 users 为例：

| id | public_id | username | password_hash | email |
|---:|---|---|---|---|
| 1 | 1234567890 | earth | BCrypt 哈希 | a@example.com |

- 表：一类数据；
- 行：一个具体对象；
- 列：对象的一个属性。

## 7.2 CRUD SQL

```sql
INSERT INTO records (title, content, user_id)
VALUES ('今天', '学习 Spring Boot', 1);

SELECT * FROM records WHERE user_id = 1;

UPDATE records
SET title = '新的标题'
WHERE id = 10 AND user_id = 1;

DELETE FROM records
WHERE id = 10 AND user_id = 1;
```

JPA 帮项目生成大量 SQL，但理解 SQL 才能判断查询是否正确和高效。

## 7.3 主键

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

对应 MySQL 自增主键。特点：

- 每行唯一；
- 适合内部关联；
- 插入时由数据库生成。

## 7.4 业务公开 ID 与数据库主键

用户同时有：

```java
private Long id;
private String publicId;
```

- `id`：数据库内部主键；
- `publicId`：10 位公开地球 ID，用于展示和好友搜索。

公开 ID 可以保持业务格式稳定，并减少直接暴露连续自增主键。

## 7.5 唯一约束

```java
@Column(nullable = false, unique = true)
private String username;
```

用户名和邮箱不能重复。Service 先查询重复用于提供友好错误，数据库唯一约束作为并发情况下的最终防线。

## 7.6 索引

索引像书的目录，可以提高查询速度，但会占空间并增加写入维护成本。

聊天表设置：

```java
@Index(name = "idx_chat_messages_sender", columnList = "sender_id")
@Index(name = "idx_chat_messages_receiver", columnList = "receiver_id")
@Index(name = "idx_chat_messages_created", columnList = "created_at")
```

这是因为聊天经常按照发送者、接收者和时间查询。

## 7.7 MySQL 题

1. 主键和唯一约束有什么区别？
2. 为什么不能给每个字段都建立索引？
3. `WHERE id = ? AND user_id = ?` 比只按 id 查询多解决了什么问题？

<details>
<summary>查看参考答案</summary>

1. 主键唯一且非空，用于标识行，一个表只有一个主键；唯一约束也防重复，一个表可以有多个。
2. 索引占空间，并增加 INSERT、UPDATE、DELETE 的维护成本。
3. 同时保证资源属于当前用户，防止越权访问其他用户记录。

</details>

---

# 第八章：JPA、Hibernate 与 Repository

## 8.1 三者关系

- JPA：Java 持久化规范，定义 Entity、Repository 等思想。
- Hibernate：项目中常用的 JPA 实现，负责生成 SQL、对象映射和脏检查。
- Spring Data JPA：在 JPA 上进一步提供 Repository 和方法名查询。

```text
业务代码
  ↓ Spring Data JPA Repository
JPA 规范
  ↓ Hibernate 实现
JDBC / MySQL Connector
  ↓
MySQL
```

## 8.2 Entity 映射

```java
@Entity
@Table(name = "records")
public class RecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5000)
    private String content;
}
```

常见注解：

| 注解 | 作用 |
|---|---|
| `@Entity` | 声明持久化实体 |
| `@Table` | 指定表名和表级约束 |
| `@Id` | 主键 |
| `@GeneratedValue` | 主键生成策略 |
| `@Column` | 列名、长度、可空、唯一等 |
| `@Enumerated` | 枚举映射 |
| `@ManyToOne` | 多对一关系 |
| `@ElementCollection` | 基础类型集合映射 |

JPA Entity 需要无参构造器，供 ORM 创建对象。

## 8.3 `JpaRepository`

```java
public interface UserRepository
        extends JpaRepository<UserEntity, Long> {
}
```

继承后自动拥有：

- `save(entity)`；
- `findById(id)`；
- `findAll()`；
- `delete(entity)`；
- `deleteById(id)`；
- `count()`。

不需要自己实现接口，Spring Data 在运行时创建代理对象。

## 8.4 方法名查询

```java
Optional<RecordEntity> findByIdAndUserId(
        Long id,
        Long userId
);
```

Spring Data 解析方法名，大致生成：

```sql
SELECT * FROM records
WHERE id = ? AND user_id = ?;
```

常见关键词：

- `findBy`；
- `And`、`Or`；
- `Between`；
- `OrderBy...Desc`；
- `IsNull`；
- `ExistsBy`。

## 8.5 JPQL 查询

记录搜索较复杂，项目使用 `@Query`：

```java
@Query("""
    select distinct record from RecordEntity record
    left join record.keywords keyword
    where record.userId = :userId
      and lower(record.content)
          like lower(concat('%', :query, '%'))
    order by record.recordDate desc
""")
```

这是 JPQL，查询的是 Entity 和属性名，不是直接查询表名和列名。

`@Param("userId")` 把参数绑定到 `:userId`。参数绑定可以避免手工拼接用户输入造成 SQL 注入。

## 8.6 Optional

```java
recordRepository.findByIdAndUserId(id, userId)
    .orElseThrow(() -> recordNotFound());
```

`Optional` 明确表达“可能没有结果”，比返回 null 后忘记检查更安全。

## 8.7 JPA 题

1. JPA 是实现还是规范？
2. `findByIdAndUserId` 为什么不需要手写实现？
3. JPQL 中应该写 Entity 名还是 MySQL 表名？
4. Repository 泛型 `<RecordEntity, Long>` 的两个类型分别是什么？

<details>
<summary>查看参考答案</summary>

1. 规范，Hibernate 是常见实现。
2. Spring Data JPA 根据方法名生成查询代理。
3. Entity 名和属性名。
4. 实体类型和主键类型。

</details>

---

# 第九章：实体关系与集合表

## 9.1 Many-to-One

聊天消息中，多条消息可以属于同一个发送者：

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "sender_id", nullable = false)
private UserEntity sender;
```

数据库中 `chat_messages.sender_id` 保存用户主键。

```text
users 1 ───── N chat_messages
```

## 9.2 `FetchType.LAZY`

懒加载表示读取聊天消息时，不一定立刻查询完整用户对象；真正访问关联属性时才按需加载。

优点是避免一次加载过多数据。风险包括：

- 在事务外访问未加载关系可能报错；
- 循环访问关系可能产生 N+1 查询；
- 直接序列化 Entity 容易触发大量查询或循环引用。

这也是项目使用 Response DTO 而不是直接返回 Entity 的原因之一。

## 9.3 `@ElementCollection`

一条记录有多个关键词：

```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(
    name = "record_keywords",
    joinColumns = @JoinColumn(name = "record_id")
)
@Column(name = "keyword", length = 30)
private List<String> keywords;
```

它把字符串集合放进独立表：

```text
record_keywords
record_id | keyword
10        | Spring
10        | Java
```

图片 Object Key 也采用类似集合表。

## 9.4 为什么图片表里不保存图片二进制

记录图片集合保存的是字符串 Object Key，例如：

```text
records/earth/550e8400-e29b-41d4-a716-446655440000.jpg
```

真正图片在 MinIO。MySQL 负责结构化元数据和业务关联。

## 9.5 关系题

1. 一个用户发送多条消息，属于什么关系？
2. `LAZY` 的作用是什么？
3. 为什么直接把带双向关联的 Entity 返回成 JSON 容易有问题？

<details>
<summary>查看参考答案</summary>

1. User 到 ChatMessage 是一对多，ChatMessage 到 User 是多对一。
2. 延迟加载关联对象，避免立即加载所有数据。
3. 可能出现循环引用、懒加载异常、额外查询和暴露内部字段。

</details>

---

# 第十章：事务

## 10.1 什么是事务

事务把一组数据库操作视为一个整体：要么全部成功，要么全部回滚。

经典 ACID：

- Atomicity 原子性；
- Consistency 一致性；
- Isolation 隔离性；
- Durability 持久性。

## 10.2 `@Transactional`

聊天发送：

```java
@Transactional
public ChatMessageResponse sendMessageAndPush(...) {
    ChatDelivery delivery = saveMessage(...);
    ...
}
```

好友删除：

```java
@Transactional
public void removeFriend(...) {
    ...
}
```

事务适合放在 Service，因为 Service 编排完整业务操作。

## 10.3 只读事务

```java
@Transactional(readOnly = true)
public List<ChatMessageResponse> getConversation(...) {
    ...
}
```

它表达该方法只查询，可以帮助框架进行语义和性能优化，但不能把它当作数据库权限隔离的唯一安全机制。

> **真实案例（建议必看）**：项目里曾经因为事记删除的 Service 方法漏写 `@Transactional`，
> 导致「删除接口返回 500、但记录没被删掉」，而同一模块的新增/修改却完全正常——
> 原因是 `save()` 属于 `SimpleJpaRepository` 自带 `@Transactional` 的方法，
> 而自己在 Repository 接口里声明的**派生删除方法**（`deleteByXxx`）不带事务，
> 它的 `em.remove()` 不会 flush，最后在关闭 EntityManager 时抛 `TransactionRequiredException`。
> 完整排查过程、日志证据、修复与验收方式见
> [`后端删除接口500与事务边界排障记录.md`](./后端删除接口500与事务边界排障记录.md)。

## 10.4 回滚

Spring 默认对未检查异常回滚。项目的 `ResponseStatusException` 属于运行时异常，通常会触发回滚。

例如接受好友请求时，如果状态更新过程抛出异常，事务可以避免数据库只完成一半。

## 10.5 数据库提交与外部推送

答辩时可进一步说明：数据库事务和 WebSocket/MinIO 并不是天然同一个分布式事务。

```text
数据库保存成功
  ↓
WebSocket 推送失败
```

此时不能说两者具备绝对原子性。项目以数据库为权威状态，客户端断线重连后通过 REST 补偿。更高可靠场景可以研究事务消息或 Outbox Pattern。

## 10.6 事务题

1. 为什么事务通常放 Service 而不是 Controller？
2. `readOnly=true` 表达什么？
3. MySQL 事务能否自动回滚已经上传到 MinIO 的对象？

<details>
<summary>查看参考答案</summary>

1. Service 表达完整业务边界，可能调用多个 Repository。
2. 该事务主要用于查询。
3. 不能。MySQL 与 MinIO 是两个独立系统，需要显式补偿或分布式一致性设计。

</details>

---

# 第十一章：数据库配置与 Hibernate ddl-auto

## 11.1 数据源配置

`application.yaml`：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/earth_online?...}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

语法：

```text
${环境变量:默认值}
```

例如没有 `DB_USERNAME` 时使用 `root`。

生产环境应通过环境变量提供密码，不要把真实密码提交进 Git。

## 11.2 `ddl-auto: update`

```yaml
jpa:
  hibernate:
    ddl-auto: update
```

开发阶段 Hibernate 会根据 Entity 尝试更新表结构，方便快速迭代。

但生产环境存在风险：

- 变更不可审查；
- 复杂迁移不可控；
- 删除或重命名字段难以安全处理；
- 多实例同时启动可能产生问题。

正式系统更适合 Flyway 或 Liquibase 管理版本化迁移。

## 11.3 SQL 日志

```yaml
show-sql: true
hibernate:
  format_sql: true
```

学习阶段可以观察 Hibernate 生成的 SQL。生产环境通常需要谨慎控制，避免日志过多或暴露敏感数据。

## 11.4 配置题

1. `${DB_PASSWORD:}` 的默认值是什么？
2. 为什么生产环境不推荐长期依赖 `ddl-auto:update`？
3. 数据库密码应该放进 Git 吗？

<details>
<summary>查看参考答案</summary>

1. 空字符串。
2. 结构变更缺少版本、审查、回滚和可预测性。
3. 不应该，应通过环境变量或密钥管理系统注入。

</details>

---

# 第十二章：Spring Security 与 JWT

## 12.1 认证和授权

- 认证 Authentication：你是谁？
- 授权 Authorization：你能做什么？

项目中：

- JWT 证明用户名；
- SecurityConfig 决定哪些 API 公开；
- Service 再检查记录归属、好友关系等业务权限。

## 12.2 登录和 Token

用户登录成功后，服务端生成 JWT：

```java
return Jwts.builder()
        .subject(username)
        .claim("userId", userId)
        .issuedAt(now)
        .expiration(expiration)
        .signWith(secretKey)
        .compact();
```

JWT 包含：

- Header：算法等元信息；
- Payload：subject、userId、签发和过期时间；
- Signature：防止内容被篡改。

JWT 通常只是签名，不等于加密。不要把密码等秘密放进 Payload。

## 12.3 请求过滤器

前端发送：

```http
Authorization: Bearer eyJ...
```

`JwtAuthenticationFilter`：

```text
读取 Authorization
  ↓
检查 Bearer 前缀
  ↓
JwtService 验证签名和过期时间
  ↓
取得 username
  ↓
创建 Authentication
  ↓
放入 SecurityContext
```

之后 Controller 可以直接接收：

```java
Authentication authentication
```

并使用：

```java
authentication.getName()
```

## 12.4 无状态会话

```java
session.sessionCreationPolicy(
    SessionCreationPolicy.STATELESS
)
```

服务端不依赖传统 HTTP Session 保存登录状态。每个受保护请求都携带 JWT。

## 12.5 公共接口

SecurityConfig 允许：

- `GET /api/health`；
- `POST /api/auth/login`；
- `POST /api/auth/register`；
- WebSocket 握手入口；
- OPTIONS 预检请求。

其他请求：

```java
.anyRequest().authenticated()
```

## 12.6 CSRF 为什么关闭

项目使用无状态 Bearer Token，而不是浏览器自动携带的 Cookie Session，因此关闭传统 CSRF 防护是常见做法。不过仍应处理 XSS、Token 存储安全、CORS 和输入校验。

## 12.7 密码安全

用户表保存：

```java
private String passwordHash;
```

不是明文密码。项目通过 `PasswordEncoder` 对注册密码进行哈希，并在登录时使用 `matches` 比较。

密码哈希和加密不同：哈希通常不可逆，验证时对候选密码执行安全比较。

## 12.8 Security 题

1. JWT Payload 是否应该保存用户明文密码？
2. `Authentication.getName()` 在项目中代表什么？
3. 为什么 JWT 验证成功后仍需检查记录属于当前用户？
4. 401 与 403 有什么区别？

<details>
<summary>查看参考答案</summary>

1. 不应该，Payload 可被读取。
2. Token 中经过签名验证的 username。
3. 认证只证明身份，资源归属检查才决定该身份是否有权访问具体记录。
4. 401 表示未通过认证；403 表示身份已知但无权执行操作。

</details>

## 12.9 答辩表达

> 项目采用无状态 JWT 认证。登录成功后后端签发包含用户名、用户 ID 和过期时间的 Token。前端每次 REST 请求通过 Bearer Header 携带 Token，JWT 过滤器验证签名和有效期后将 Authentication 放入 SecurityContext。Controller 从 Authentication 获取可信用户名，Service 再执行资源归属和好友关系等细粒度授权。

---

# 第十三章：数据权限与越权防护

## 13.1 典型越权风险

假设用户 A 拥有记录 10，用户 B 修改 URL：

```text
GET /api/records/10
```

如果后端只执行：

```java
findById(10)
```

B 可能读到 A 的数据，这叫水平越权或 IDOR。

## 13.2 项目防护方式

```java
findByIdAndUserId(id, currentUserId)
```

对应条件：

```sql
WHERE id = :id AND user_id = :currentUserId
```

即使攻击者猜到 ID，也无法查询其他用户的记录。

## 13.3 为什么 userId 不能来自请求体

不安全：

```json
{
  "userId": 1,
  "content": "..."
}
```

前端字段可以篡改。项目从 JWT 对应的 Authentication 获取用户名，再在后端查询用户 ID。

## 13.4 图片对象名权限

记录图片要求前缀：

```java
String expectedPrefix = "records/" + username + "/";
```

聊天图片要求：

```text
chat/{username}/...
```

头像要求：

```text
avatars/{username}/...
```

这可以防止用户把其他用户的 Object Key 直接绑定到自己的数据中。成熟系统还可以调用 MinIO 检查对象是否实际存在并加强授权策略。

## 13.5 权限题

1. 前端隐藏删除按钮能否防止越权删除？
2. 为什么项目查询单条记录使用 `id + userId`？
3. Object Key 前缀校验解决什么问题？

<details>
<summary>查看参考答案</summary>

1. 不能，攻击者可以直接调用 API。
2. 同时确认记录存在且属于当前用户。
3. 防止当前用户引用其他用户目录下的图片对象。

</details>

---

# 第十四章：MinIO 基础

## 14.1 MinIO 是什么

MinIO 是兼容 Amazon S3 API 的对象存储。它适合保存：

- 图片；
- 视频；
- 文档；
- 备份；
- 其他非结构化二进制对象。

对象存储不是传统磁盘路径共享，也不是关系型数据库。

## 14.2 核心术语

| 术语 | 含义 | 项目例子 |
|---|---|---|
| Endpoint | MinIO 服务地址 | `http://localhost:9000` |
| Access Key | 访问身份 | 环境变量配置 |
| Secret Key | 访问密钥 | 环境变量配置 |
| Bucket | 对象容器 | `earth-online-records` |
| Object Key | Bucket 内对象的唯一名字 | `records/alice/uuid.jpg` |
| Presigned URL | 临时签名访问地址 | 一小时有效预览链接 |

Object Key 中的斜杠是逻辑前缀，看起来像目录，但对象存储本质上使用键标识对象。

## 14.3 为什么使用 MinIO，而不是 MySQL BLOB

项目的设计：

```text
MySQL：用户、记录、图片 Object Key、业务关系
MinIO：真正的图片二进制内容
```

好处：

- 数据库表更轻；
- 图片可独立扩展和备份；
- 更适合大文件传输；
- 可使用 S3 工具和预签名 URL；
- Web 服务不必每次把文件完整读入数据库。

MySQL 仍负责说明“哪条记录引用哪些对象”。

## 14.4 项目配置

```yaml
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  public-url: ${MINIO_PUBLIC_URL:}
  access-key: ${MINIO_ACCESS_KEY:}
  secret-key: ${MINIO_SECRET_KEY:}
  bucket: earth-online-records
```

`MinioConfig` 把配置装配成单例 `MinioClient` Bean。

## 14.5 MinIO 题

1. Bucket 和 Object Key 有什么区别？
2. Object Key 是 Windows 本地绝对路径吗？
3. 为什么数据库仍需要保存 Object Key？

<details>
<summary>查看参考答案</summary>

1. Bucket 是对象容器，Object Key 是容器内单个对象的唯一名称。
2. 不是，它是对象存储中的逻辑键。
3. 用于建立业务数据与图片对象之间的关联。

</details>

---

# 第十五章：项目图片上传完整链路

## 15.1 第一步：前端发送 multipart/form-data

上传文件不能像普通 JSON 那样只传文本，前端通常创建 `FormData`：

```text
POST /api/uploads/images
Content-Type: multipart/form-data
Authorization: Bearer ...
file: 图片二进制
```

## 15.2 第二步：UploadController 接收

```java
@PostMapping(
    value = "/images",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
public Map<String, String> uploadImage(
        Authentication authentication,
        @RequestParam("file") MultipartFile file
) {
    String objectKey = storageService.uploadRecordImage(
            authentication.getName(),
            file
    );
    return Map.of("objectKey", objectKey);
}
```

关键点：

- `MultipartFile` 表示上传文件；
- 用户名来自 Authentication；
- Controller 不直接操作 MinIO，交给 StorageService。

## 15.3 第三步：文件校验

项目限制：

```java
private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
    "image/jpeg",
    "image/png",
    "image/webp"
);
```

校验包括：

- 文件非空；
- 最大 5MB；
- MIME 类型在白名单；
- 文件名包含扩展名。

答辩中可以承认：仅相信客户端 MIME 类型仍不够强，生产级系统还可以读取文件魔数、重新编码图片、病毒扫描并限制像素尺寸。

## 15.4 第四步：保证 Bucket 存在

```java
boolean exists = minioClient.bucketExists(...);
if (!exists) {
    minioClient.makeBucket(...);
}
```

项目在上传时按需检查并创建 Bucket。

高并发下还可以考虑初始化阶段创建、权限控制和并发创建处理。

## 15.5 第五步：生成 Object Key

```java
String objectName = folder + "/"
        + username + "/"
        + UUID.randomUUID() + extension;
```

示例：

```text
records/alice/2dc1...-....jpg
avatars/alice/3ab2...-....webp
chat/alice/9ce4...-....png
```

使用 UUID 的作用：

- 降低重名覆盖风险；
- 不直接信任用户原始文件名；
- 便于生成唯一对象。

## 15.6 第六步：上传对象

```java
minioClient.putObject(
    PutObjectArgs.builder()
        .bucket(bucket)
        .object(objectName)
        .stream(file.getInputStream(), file.getSize(), -1L)
        .contentType(file.getContentType())
        .build()
);
```

上传成功后返回 Object Key，而不是永久公开 URL。

## 15.7 第七步：记录保存 Object Key

图片上传和发布记录是两个请求：

```text
上传图片 → 得到 objectKey
  ↓
发布记录时把 objectKey 放入 photos
  ↓
Service 检查 records/{username}/ 前缀
  ↓
MySQL 保存 objectKey
```

这样记录表只保存小字符串。

## 15.8 上传链路题

请按顺序排列：

- MySQL 保存 Object Key
- Controller 接收 MultipartFile
- MinIO putObject
- 前端先上传图片
- Service 校验文件
- 生成 UUID Object Key

<details>
<summary>查看参考答案</summary>

前端先上传图片 → Controller 接收 MultipartFile → Service 校验文件 → 生成 UUID Object Key → MinIO `putObject` → 发布记录时 MySQL 保存 Object Key。

</details>

---

# 第十六章：预签名 URL 与图片访问

## 16.1 为什么不直接公开 Bucket

如果 Bucket 完全公开，知道 Object Key 的人可能长期访问图片。项目使用预签名 URL：

```java
minioClient.getPresignedObjectUrl(
    GetPresignedObjectUrlArgs.builder()
        .method(Method.GET)
        .bucket(bucket)
        .object(objectKey)
        .expiry(1, TimeUnit.HOURS)
        .build()
);
```

URL 中包含签名和过期信息，一小时后失效。

## 16.2 Object Key 与预签名 URL 的区别

| Object Key | 预签名 URL |
|---|---|
| 稳定保存到 MySQL | 临时返回给前端 |
| 不是可直接展示的完整地址 | 浏览器可以访问 |
| 不包含临时签名 | 包含签名和过期时间 |
| 适合持久化 | 不适合长期持久化 |

不能把一小时预签名 URL 当作永久数据存入数据库，否则过期后图片无法访问。

## 16.3 public-url

MinIO 可能在 Docker 内部使用一个地址，而浏览器需要通过 Nginx 公网地址访问。项目可以使用 `MINIO_PUBLIC_URL` 替换签名 URL 的访问源，同时保留对象路径和查询签名。

## 16.4 降级处理

```java
public String getPreviewUrlOrNull(String objectKey) {
    try {
        return getPreviewUrl(objectKey);
    } catch (...) {
        return null;
    }
}
```

设计目标：MinIO 暂停时，资料、好友、记录和聊天 API 仍尽量返回结构化数据，只是图片预览为空。

这体现了“图片展示是可降级能力，核心业务数据仍在 MySQL”。

## 16.5 预签名 URL 题

1. 为什么预签名 URL 不适合永久存入 MySQL？
2. MinIO 不可用时，为什么项目部分查询仍返回成功而图片为 null？
3. 预签名 URL 到期是否意味着 MinIO 中的对象被删除？

<details>
<summary>查看参考答案</summary>

1. 它有过期时间。
2. 项目将预览视为可降级数据，`getPreviewUrlOrNull` 捕获异常并返回 null。
3. 不意味着。只是该访问凭证失效，对象仍然存在。

</details>

## 16.6 答辩表达

> 项目采用 MySQL 保存图片 Object Key、MinIO 保存二进制文件的分离方案。上传时服务端校验大小和 MIME 类型，以用户名和 UUID 生成对象名并写入 Bucket。业务记录只保存稳定 Object Key；查询时后端生成一小时有效的预签名 URL供浏览器预览，从而避免永久公开 Bucket。

---

# 第十七章：完整追踪“发布一条带图记录”

## 17.1 图片上传阶段

```text
Vue 选择图片
  ↓ multipart/form-data
POST /api/uploads/images
  ↓ JWT Filter
Authentication 建立
  ↓ UploadController
取得 username 和 MultipartFile
  ↓ StorageService
校验大小、类型、扩展名
  ↓ MinioClient.putObject
  ↓
返回 objectKey
```

## 17.2 记录保存阶段

```text
Vue 组装 JSON，photos 放 objectKey
  ↓
POST /api/records
  ↓ JWT Filter
  ↓ RecordController
@Valid 校验 DTO
  ↓ RecordService
取得当前 userId
校验 records/{username}/ 前缀
规范化标题和事件类型
  ↓ RecordRepository.save
  ↓ Hibernate 生成 SQL
  ↓ MySQL records + record_photos
```

## 17.3 响应阶段

```text
保存后的 RecordEntity
  ↓ Service.toResponse
Object Key → MinIO 预签名 URL
  ↓ Jackson 序列化
RecordResponse JSON
  ↓
Vue/Pinia 更新页面
```

## 17.4 为什么拆成两个请求

好处：

- 文件上传与记录 JSON 逻辑分开；
- 上传失败可在发布前提示；
- 同一上传能力可用于头像、记录、聊天；
- 记录 DTO 不需要承载大块二进制。

代价：

- 上传成功但记录发布失败时，可能产生孤儿对象；
- 需要定期清理未被引用的对象，或使用临时上传状态和确认机制。

答辩中主动说出这个一致性问题会体现工程理解。

## 17.5 综合题

如果 MinIO 上传成功，但 MySQL 保存记录失败，会发生什么？如何改进？

<details>
<summary>查看参考答案</summary>

MinIO 中会留下没有业务记录引用的孤儿对象。MySQL 事务不能自动回滚 MinIO。可以在保存失败时补偿删除对象、将上传记入临时对象表并定期清理，或使用确认状态和后台清理任务。

</details>

---

# 第十八章：完整追踪登录与受保护请求

## 18.1 注册

```text
RegisterRequest 校验
  ↓
检查用户名和邮箱重复
  ↓
PasswordEncoder 哈希密码
  ↓
生成公开 publicId
  ↓
UserRepository.save
  ↓
返回 UserResponse，不返回 passwordHash
```

## 18.2 登录

```text
按用户名查 UserEntity
  ↓
PasswordEncoder.matches(明文, 哈希)
  ↓
JwtService.generateToken
  ↓
返回 accessToken 和用户信息
```

## 18.3 后续请求

```text
Authorization: Bearer token
  ↓
JwtAuthenticationFilter
  ↓
JwtService.extractUsername
  ↓
SecurityContextHolder.setAuthentication
  ↓
SecurityFilterChain 判断 authenticated
  ↓
Controller Authentication.getName
  ↓
Service 进行资源权限检查
```

## 18.4 Token 无效

过滤器解析失败时不建立身份，继续过滤器链。后续 `.anyRequest().authenticated()` 发现没有认证，使用 AuthenticationEntryPoint 返回统一 401 JSON。

## 18.5 登录题

1. 密码验证时是否解密数据库里的 passwordHash？
2. JWT 过滤器建立身份后，Controller 从哪里读取用户名？
3. Token 过期后为什么受保护接口返回 401？

<details>
<summary>查看参考答案</summary>

1. 不是解密，而是使用 PasswordEncoder 的安全匹配算法比较。
2. `Authentication`，其来源是 SecurityContext。
3. JWT 解析失败，过滤器不建立 Authentication，安全链拒绝未认证请求。

</details>

---

# 第十九章：聊天消息如何落库

## 19.1 ChatMessageEntity

主要字段：

```text
id
sender_id
receiver_id
content
message_type
image_object_key
client_message_id
created_at
read_at
```

发送者和接收者使用 `@ManyToOne` 关联 UserEntity。

## 19.2 幂等唯一约束

```java
@UniqueConstraint(
    name = "uk_chat_sender_client_message",
    columnNames = {
        "sender_id",
        "client_message_id"
    }
)
```

同一个发送者重复提交同一个 `clientMessageId` 时，不应插入两条消息。

Service 先查询已有记录，数据库唯一约束再作为并发最终防线。

## 19.3 先落库再推送

```text
验证发送者和好友关系
  ↓
检查 clientMessageId
  ↓
保存 ChatMessageEntity
  ↓
构造双方视角 DTO
  ↓
WebSocket 推送
```

WebSocket 负责实时送达，MySQL 负责历史消息和未读状态。

## 19.4 已读状态

接收者打开会话后，将符合条件的消息设置：

```text
read_at = 当前时间
```

然后通过 WebSocket 向发送者发送已读回执。

## 19.5 聊天题

1. 为什么 WebSocket 不能替代聊天消息表？
2. `clientMessageId` 和数据库自增 id 分别由谁生成？
3. 为什么唯一键包含 sender_id？

<details>
<summary>查看参考答案</summary>

1. WebSocket 负责实时通信，不负责历史和离线持久化。
2. 前者由客户端生成，后者由数据库生成。
3. 让不同发送者拥有各自的幂等 ID 空间。

</details>

---

# 第二十章：性能、分页与常见数据库问题

## 20.1 为什么需要分页

如果一个用户有十万条记录：

```java
findAllByUserId(userId)
```

一次加载全部会造成：

- SQL 查询慢；
- 内存占用大；
- JSON 响应巨大；
- 前端渲染卡顿。

生产优化方向：

```java
Page<RecordEntity> findAllByUserId(
    Long userId,
    Pageable pageable
);
```

## 20.2 N+1 查询

查询 100 条消息后，再为每条消息分别查询发送者，可能产生：

```text
1 条主查询 + 100 条关联查询
```

解决方向：

- fetch join；
- EntityGraph；
- DTO 投影；
- 合理批量加载；
- 根据实际 SQL 分析，而不是盲目改为 EAGER。

## 20.3 模糊搜索

```sql
LIKE '%关键词%'
```

数据量较小时简单有效，但前置 `%` 往往难以使用普通 B-Tree 索引。数据量大后可考虑：

- MySQL FULLTEXT；
- Elasticsearch/OpenSearch；
- 专门的搜索字段和索引策略。

## 20.4 索引选择

应根据真实查询设计组合索引。例如经常执行：

```sql
WHERE user_id = ?
ORDER BY record_date DESC
```

可以评估 `(user_id, record_date)` 组合索引，并使用 `EXPLAIN` 查看执行计划。

## 20.5 性能题

1. 为什么不能一次返回用户全部聊天记录？
2. N+1 是什么？
3. `LIKE '%abc%'` 在大表上有什么潜在问题？

<details>
<summary>查看参考答案</summary>

1. 数据量增长后会增加数据库、内存、网络和前端渲染压力，应分页或游标查询。
2. 一条主查询后，为每个结果再执行一条关联查询。
3. 普通索引可能无法有效使用，导致扫描大量数据。

</details>

---

# 第二十一章：配置、安全和部署答辩点

## 21.1 环境变量

项目配置包括：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MS
MINIO_ENDPOINT
MINIO_PUBLIC_URL
MINIO_ACCESS_KEY
MINIO_SECRET_KEY
```

真实密钥不应写入源码或提交仓库。

## 21.2 最小权限

生产环境不应长期使用 MySQL root。应创建专用数据库账号，只授予项目所需权限。

MinIO Access Key 也应使用专用账号和 Bucket Policy，限制只能访问所需 Bucket/前缀。

## 21.3 备份

完整备份必须同时考虑：

- MySQL 数据；
- MinIO 对象；
- 配置和密钥的安全备份；
- 两者时间点的一致性。

只备份 MySQL 会失去图片，只备份 MinIO 会失去业务关系。

## 21.4 健康检查

项目提供公开的：

```text
GET /api/health
```

可用于判断 Web 应用是否响应。更成熟的方案可以使用 Spring Boot Actuator，并分别检查数据库、MinIO 等依赖健康状态。

## 21.5 日志

日志应记录：

- 请求失败原因；
- 外部服务错误；
- 关键业务状态；
- 必要的追踪 ID。

日志不应记录：

- 明文密码；
- 完整 JWT；
- MinIO Secret Key；
- 数据库密码；
- 不必要的隐私内容。

---

# 第二十二章：常见错误排查

## Spring Boot 无法启动

检查：

1. Java 版本是否满足项目要求；
2. 8080 是否被占用；
3. `JWT_SECRET` 是否配置且长度满足算法要求；
4. Maven 依赖是否下载成功；
5. MySQL 地址、账号和密码是否正确；
6. 启动日志中的第一个根因异常。

## MySQL 连接失败

检查：

1. MySQL 服务是否启动；
2. 3306 是否监听；
3. `earth_online` 数据库是否存在；
4. 用户名密码是否正确；
5. JDBC URL 的主机名是否适合当前 Docker/本机环境；
6. 账号是否有权限；
7. 字符集和时区参数。

## 接口返回 401

检查：

1. Authorization Header 是否存在；
2. 是否为 `Bearer ` 前缀；
3. Token 是否过期；
4. JWT Secret 是否在服务重启后变化；
5. 是否错误调用了需要登录的接口。

## 接口返回 403

身份可能有效，但业务权限不足。检查：

- 是否为好友；
- 是否操作自己的资源；
- Security 规则是否允许；
- 当前 Authentication 是否正确。

## 图片上传失败

检查：

1. MinIO 是否启动；
2. Endpoint、Access Key、Secret Key 是否正确；
3. Bucket 权限；
4. 文件是否超过 5MB；
5. MIME 类型是否为 JPEG、PNG、WebP；
6. multipart 字段名是否为 `file`；
7. 后端和 Nginx 上传大小限制。

## 图片上传成功但无法显示

检查：

1. MySQL 中是否保存正确 Object Key；
2. Object Key 前缀是否匹配用户名；
3. 预签名 URL 是否过期；
4. `MINIO_PUBLIC_URL` 是否为浏览器可访问地址；
5. Nginx 是否正确代理对象路径；
6. MinIO 系统时间是否正确，签名对时间敏感。

---

# 第二十三章：综合做题

## 题目 1：分层追踪

用户请求 `GET /api/records/12`，请描述完整链路。

<details>
<summary>参考答案</summary>

1. 请求先经过 Security 过滤器链。
2. JWT Filter 验证 Token 并建立 Authentication。
3. `RecordController.getById` 读取 Authentication 和路径变量 12。
4. Controller 调用 `RecordService.findById(username, 12)`。
5. Service 根据用户名取得当前 userId。
6. Repository 执行 `findByIdAndUserId(12, userId)`，同时检查归属。
7. 找不到则抛 404；找到则转换为 RecordResponse。
8. 图片 Object Key 被转换为临时预签名 URL。
9. Jackson 把 Response DTO 序列化成 JSON 返回。

</details>

## 题目 2：数据库设计

为什么 `users.username` 既在 Service 中检查重复，又在数据库加 unique？

<details>
<summary>参考答案</summary>

Service 检查用于提前返回友好错误，但两个并发请求可能同时通过检查。数据库唯一约束是最终一致性防线，保证任何情况下都不能插入重复用户名。

</details>

## 题目 3：安全改错

下面接口有什么安全问题？

```java
@DeleteMapping("/{id}")
public void delete(@PathVariable Long id) {
    recordRepository.deleteById(id);
}
```

<details>
<summary>参考答案</summary>

没有认证用户和资源归属判断。登录用户只要猜到其他记录 ID 就可能删除。应从 Authentication 获取当前用户，并按 `id + userId` 查询/删除，找不到时返回 404。

</details>

## 题目 4：JPA 与 SQL

下面 Repository 方法大致对应什么 SQL？

```java
Optional<RecordEntity> findByIdAndUserId(
    Long id,
    Long userId
);
```

<details>
<summary>参考答案</summary>

```sql
SELECT * FROM records
WHERE id = ? AND user_id = ?;
```

实际列选择和 SQL 形式由 Hibernate 方言决定。

</details>

## 题目 5：MinIO 设计

为什么数据库保存 Object Key，而接口返回预签名 URL？

<details>
<summary>参考答案</summary>

Object Key 稳定，适合长期持久化；预签名 URL 带临时签名会过期，只适合返回给浏览器短期访问。每次查询可根据 Object Key 重新生成 URL。

</details>

## 题目 6：事务边界

一个 `@Transactional` 方法先写 MySQL，再上传 MinIO，上传失败时是否能保证 MinIO 和 MySQL 自动一起回滚？

<details>
<summary>参考答案</summary>

不能。Spring 的数据库事务主要管理数据库连接，MinIO 是独立外部系统。数据库可以回滚，但已完成的对象操作需要显式补偿；反过来也可能产生孤儿对象。

</details>

## 题目 7：接口设计

为什么上传头像、记录图片和聊天图片使用不同 Object Key 前缀？

<details>
<summary>参考答案</summary>

便于分类、权限校验、生命周期管理和清理。Service 可以根据 `avatars/`、`records/`、`chat/` 判断对象用途和所有者，降低跨业务误用风险。

</details>

## 题目 8：异常处理

数据库记录不存在，Service 抛出 `ResponseStatusException(NOT_FOUND)` 后，如何变成统一 JSON？

<details>
<summary>参考答案</summary>

异常传播到 `GlobalExceptionHandler`，其 `@ExceptionHandler(ResponseStatusException.class)` 读取状态码和 reason，构造 `ApiErrorResponse` 并通过 `ResponseEntity` 返回 404 JSON。

</details>

## 题目 9：离线与降级

为什么 MinIO 停止后，项目仍希望用户资料和记录文本可以查询？

<details>
<summary>参考答案</summary>

文本、用户和记录关系存储在 MySQL，图片预览只是附加能力。`getPreviewUrlOrNull` 将图片故障降级为 null，避免非核心依赖故障扩大成整个业务 API 不可用。

</details>

## 题目 10：架构设计

如果图片删除功能上线，需要考虑哪些一致性问题？

<details>
<summary>参考答案</summary>

需要确认对象属于当前用户且没有其他业务引用；决定先删数据库引用还是先删 MinIO；处理其中一步成功另一步失败；通过补偿、状态表或后台任务清理；记录审计日志；避免删除默认头像或其他用户对象。

</details>

---

# 第二十四章：答辩高频问答

## Q1：为什么选择 Spring Boot？

> Spring Boot 生态完整，能够通过自动配置和 Starter 快速整合 MVC、Security、JPA、Validation 和 WebSocket。项目采用分层架构，业务模块清晰，便于开发、测试和后续扩展。

## Q2：项目后端如何分层？

> Controller 处理 HTTP 和 DTO，Service 负责业务规则、资源权限和事务，Repository 使用 Spring Data JPA 访问 MySQL，Entity 映射数据库表，Response DTO 对外提供稳定数据结构。

## Q3：JPA 和 MySQL 是什么关系？

> MySQL 是实际关系型数据库；JPA 是 Java 持久化规范；Hibernate 是 JPA 实现；Spring Data JPA 提供 Repository 抽象。最终数据仍通过 JDBC Driver 写入 MySQL。

## Q4：为什么不用原生 JDBC？

> JPA/Repository 减少重复 SQL 和对象转换代码，并提供实体映射、事务和方法名查询。复杂查询仍可以使用 JPQL、原生 SQL，并需要结合执行计划优化。

## Q5：为什么 Entity 和 DTO 分开？

> Entity 面向数据库，DTO 面向接口。分开可以防止客户端修改内部字段、避免暴露密码哈希和关系对象、减少懒加载与循环序列化问题，也能独立演进数据库和 API。

## Q6：如何防止用户访问别人的记录？

> 用户身份来自经过签名验证的 JWT，而不是请求体。查询和修改记录时同时使用记录 ID 与当前用户 ID，例如 `findByIdAndUserId`。即使用户猜到其他记录 ID，也无法读取或修改。

## Q7：为什么 JWT 是无状态的？

> 服务端不保存传统 Session，每次请求都携带 Token。过滤器验证签名和过期时间后建立 Authentication。这样便于服务扩展，但 Token 吊销、刷新和安全存储需要额外设计。

## Q8：密码如何保存？

> 数据库只保存 PasswordEncoder 生成的密码哈希，不保存明文。登录时使用安全的 matches 比较，而不是解密原密码。

## Q9：为什么选择 MinIO？

> 图片属于非结构化对象，MinIO 提供兼容 S3 的对象存储能力，适合文件上传、独立扩展和临时签名访问。MySQL 只保存 Object Key 和业务关联，使数据库保持轻量。

## Q10：图片上传过程是什么？

> 前端以 multipart 上传，后端从 Authentication 获取用户身份，校验文件大小和类型，以业务目录、用户名和 UUID 生成 Object Key，写入 MinIO，然后返回 Object Key。发布记录时再把 Key 保存进 MySQL。

## Q11：为什么返回预签名 URL？

> Bucket 不需要永久公开。后端根据 Object Key 生成有时效的签名 URL，浏览器短期访问。一小时过期后可以重新请求生成，数据库始终保存稳定 Key。

## Q12：MySQL 和 MinIO 如何保证事务一致性？

> 当前两者不属于同一个本地事务。上传成功而业务保存失败时可能产生孤儿对象。项目可通过失败补偿、临时对象状态和定时清理改进。核心数据库操作使用 Spring 事务保证自身一致性。

## Q13：`ddl-auto:update` 是否适合生产？

> 它适合开发阶段快速同步实体结构。生产环境应使用 Flyway 或 Liquibase 进行可审查、可追踪、可回滚规划的版本化迁移。

## Q14：项目如何处理异常？

> Service 用 `ResponseStatusException` 表达业务错误，`@RestControllerAdvice` 统一转换为包含 status、code、message 和 timestamp 的 JSON；未预期异常返回通用 500，详细信息只保留在日志。

## Q15：项目数据库有哪些优化空间？

> 可以增加分页、按查询模式设计组合索引、分析 N+1、为全文搜索引入合适索引或搜索引擎，并用 Flyway 管理结构变更。优化应结合数据量、慢 SQL 和 EXPLAIN，而不是盲目加索引。

## Q16：为什么先落库再 WebSocket 推送？

> MySQL 是聊天消息的权威数据源，WebSocket 只负责实时传输。先落库可以保证断线后仍能查询历史，推送失败时客户端重连后还能通过 REST 补偿。

---

# 第二十五章：答辩模拟题

先口头回答，每题控制在 30～60 秒，再展开参考要点。

## 模拟题 1

请用一分钟介绍项目后端架构。

<details>
<summary>回答要点</summary>

- Spring Boot 作为后端框架；
- Controller/Service/Repository 分层；
- Spring Security + JWT 无状态认证；
- Spring Data JPA + MySQL 持久化；
- MinIO 保存图片对象；
- WebSocket/STOMP 实时推送；
- REST 负责 CRUD 和断线补偿。

</details>

## 模拟题 2

请说明一条带图片记录从浏览器到存储的全过程。

<details>
<summary>回答要点</summary>

- 先 multipart 上传图片；
- JWT 验证用户；
- 校验大小和 MIME；
- UUID Object Key 上传 MinIO；
- 前端获得 Key；
- 再 POST 记录 JSON；
- Service 校验 Key 前缀；
- JPA 将记录和 Key 保存到 MySQL；
- 查询时生成预签名 URL。

</details>

## 模拟题 3

项目有哪些安全设计？

<details>
<summary>回答要点</summary>

- 密码哈希；
- JWT 签名和过期时间；
- Security 过滤器链；
- 默认接口要求认证；
- 身份从 Authentication 获取；
- 按用户 ID 限制资源查询；
- 好友关系业务授权；
- DTO 校验；
- 图片大小、类型、前缀校验；
- 密钥使用环境变量。

</details>

## 模拟题 4

项目当前有哪些不足？

<details>
<summary>回答要点</summary>

可以诚实回答并给出改进：

- `ddl-auto:update` 更适合开发，可换 Flyway；
- 记录和聊天数据需要分页；
- 文件 MIME 校验可增加魔数检测；
- MinIO 与 MySQL 存在跨系统一致性问题，可增加清理与补偿；
- JWT 可增加 Refresh Token 和吊销策略；
- SQL 日志生产环境应收敛；
- 数据量增长后需分析索引、N+1 和搜索方案；
- 可以增加单元测试、集成测试和监控。

答辩时不要只说“没有问题”，指出边界并提出合理方案更可信。

</details>

---

# 第二十六章：动手学习任务

## 任务 1：观察一次请求

1. 启动 MySQL 和后端。
2. 登录获取 Token。
3. 在浏览器 Network 查看 `/api/records`。
4. 找出请求方法、Header、响应状态和 JSON。
5. 在后端打断点观察 Controller → Service → Repository。

## 任务 2：观察 JPA SQL

1. 保持 `show-sql: true`。
2. 查询一条记录。
3. 找出 Hibernate 输出的 SELECT。
4. 对照 Repository 方法名。
5. 说明 WHERE 中为什么包含 user_id。

## 任务 3：验证 DTO 校验

依次尝试：

- content 为空；
- content 超长；
- 关键词超过 5 个；
- 图片超过 9 个。

观察是否返回 400 和统一错误 JSON。

## 任务 4：验证数据权限

1. 准备两个账号。
2. 用账号 A 创建记录。
3. 使用账号 B 的 Token 请求 A 的记录 ID。
4. 确认后端不能返回 A 的数据。

只能在自己的测试环境执行，不要测试未获授权的系统。

## 任务 5：观察 MinIO

1. 上传一张图片。
2. 在 MinIO Console 找到对象。
3. 观察 Bucket、Object Key、Content-Type 和大小。
4. 查看 MySQL 中保存的是 Key 还是图片二进制。
5. 查看接口返回的预签名 URL。
6. 对比 Object Key 与 URL。

## 任务 6：验证降级

1. 先确保已有带图记录。
2. 停止 MinIO。
3. 查询记录。
4. 观察文本数据是否仍然返回、图片 URL 如何变化。
5. 查看后端日志。
6. 恢复 MinIO 后重新请求。

## 任务 7：画三张答辩图

独立画出：

1. Controller → Service → Repository → MySQL 分层图；
2. JWT Filter → SecurityContext → Authentication 鉴权图；
3. 浏览器 → UploadController → MinIO → Object Key → MySQL → 预签名 URL 图片链路图。

如果能不看文档解释这三张图，就已经建立了项目后端的整体认识。

---

# 第二十七章：最终掌握检查表

## Spring Boot

- [ ] 知道 `@SpringBootApplication` 的作用。
- [ ] 理解 Bean、IoC 和构造器注入。
- [ ] 能区分 Controller、Service、Repository、Entity、DTO。
- [ ] 会解释 REST 注解和 HTTP 状态码。
- [ ] 理解 `@Valid` 和全局异常处理。
- [ ] 知道配置如何从环境变量读取。
- [ ] 能说明事务边界和回滚。

## MySQL 与 JPA

- [ ] 理解表、行、列、主键、唯一约束和索引。
- [ ] 能解释 JPA、Hibernate、Spring Data JPA 的关系。
- [ ] 能看懂 Entity 映射。
- [ ] 能看懂 Repository 方法名查询。
- [ ] 理解 `ManyToOne` 和 `ElementCollection`。
- [ ] 知道为什么按 id + userId 查询。
- [ ] 理解分页、N+1 和模糊搜索问题。
- [ ] 知道 `ddl-auto:update` 的生产风险。

## Security

- [ ] 能区分认证和授权。
- [ ] 能描述 JWT 从生成到解析的过程。
- [ ] 知道 JWT Payload 不是秘密区域。
- [ ] 能解释 Security Filter Chain。
- [ ] 知道密码保存哈希而不是明文。
- [ ] 能区分 401 和 403。
- [ ] 知道前端权限控制不能代替后端。

## MinIO

- [ ] 能解释 Bucket、Object Key 和预签名 URL。
- [ ] 能描述图片上传全过程。
- [ ] 知道 MySQL 保存 Key，MinIO 保存二进制。
- [ ] 知道预签名 URL 不应永久保存。
- [ ] 理解 Object Key 前缀权限。
- [ ] 理解 MySQL 与 MinIO 的一致性边界。
- [ ] 能提出孤儿对象清理方案。

## 答辩

- [ ] 能在一分钟内说明后端架构。
- [ ] 能追踪一次记录创建请求。
- [ ] 能追踪一次带图记录发布。
- [ ] 能追踪一次 JWT 认证。
- [ ] 能解释聊天为什么先落库再推送。
- [ ] 能说明项目优点、边界和后续优化方案。

---

# 附录：一句话知识卡片

1. Spring Boot：通过自动配置和 Starter 快速构建 Spring 应用。
2. Bean：由 Spring 容器创建和管理的对象。
3. IoC：对象创建和组装的控制权交给容器。
4. DI：容器把对象所需依赖注入进来。
5. Controller：处理 HTTP 边界。
6. Service：承载业务规则和事务。
7. Repository：表达数据访问。
8. Entity：Java 对象与数据库表的映射。
9. DTO：接口输入输出的数据模型。
10. JPA：Java 持久化规范。
11. Hibernate：JPA 的一种实现。
12. Spring Data JPA：提供 Repository 抽象和查询生成。
13. MySQL：保存结构化业务数据。
14. 主键：唯一标识一行数据。
15. 索引：用额外空间和写入成本换取查询速度。
16. 事务：一组数据库操作整体成功或回滚。
17. Authentication：已经确认的当前用户身份。
18. JWT：带签名和过期时间的无状态身份凭证。
19. Password Hash：不可逆保存密码验证信息。
20. MinIO：兼容 S3 API 的对象存储。
21. Bucket：对象容器。
22. Object Key：对象在 Bucket 内的唯一名字。
23. 预签名 URL：具有权限和有效期的临时对象访问地址。
24. 降级：非核心依赖失败时尽量保留核心功能。
25. 幂等：同一操作重复执行不会产生额外业务结果。

