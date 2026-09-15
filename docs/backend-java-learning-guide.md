# Earth Online 后端 Java 学习指南

> 面向刚开始阅读 Spring Boot 项目的同学。本文结合 `backend/src/main/java/com/earthonline/backend` 中的真实代码，解释项目涉及的后端知识点、代码分层和请求执行流程。

## 1. 后端整体作用

Earth Online 后端是一个 Java 21 + Spring Boot 应用，主要负责：

- 用户注册、登录和 JWT 身份认证
- 用户资料和头像管理
- 生活记录的增删改查、筛选和分页
- 好友申请和好友关系管理
- 私聊历史记录和实时 WebSocket 聊天
- MinIO 图片上传
- 未来信件的保存、封存和开启
- 年度统计、AI 年度总结和年度回忆问答

后端程序的启动类是：

```text
backend/src/main/java/com/earthonline/backend/BackendApplication.java
```

启动方法中的 `SpringApplication.run(...)` 会创建 Spring 容器，扫描组件，并启动 Web 服务。

## 2. Maven 和项目配置

### 2.1 `pom.xml`

`backend/pom.xml` 是 Maven 项目配置文件，作用类似前端的 `package.json`：

- 声明 Java 和 Spring Boot 版本
- 声明第三方依赖
- 管理编译、测试和打包

项目中比较重要的依赖：

| 依赖 | 知识点 |
| --- | --- |
| `spring-boot-starter-webmvc` | REST API、Controller、HTTP 请求 |
| `spring-boot-starter-data-jpa` | ORM、Entity、Repository、数据库查询 |
| `mysql-connector-j` | 连接 MySQL |
| `spring-boot-starter-security` | 认证和权限控制 |
| `spring-security-crypto` | 密码哈希，例如 BCrypt |
| `jjwt-*` | JWT 的生成和解析 |
| `spring-boot-starter-websocket` | WebSocket 和 STOMP |
| `spring-boot-starter-validation` | `@Valid`、请求参数校验 |
| `minio` | 对象存储文件上传 |
| Jackson | Java 对象和 JSON 的转换 |

### 2.2 `application.yaml`

文件位置：

```text
backend/src/main/resources/application.yaml
```

它集中配置：

- MySQL 地址、用户名和密码
- JPA 的数据库行为
- JWT 密钥和过期时间
- 后端端口 `8080`
- MinIO 地址和桶名
- DeepSeek API 地址、模型和超时时间
- WebSocket 允许的前端 Origin

配置中的写法：

```yaml
password: ${DB_PASSWORD:}
```

表示优先读取环境变量 `DB_PASSWORD`；如果没有，就使用冒号后的默认值。密钥、数据库密码和 AI API Key 不应直接硬编码到源码中。

## 3. Spring Boot 基础概念

### 3.1 Spring 容器和 Bean

Spring 会管理带有以下注解的类：

- `@Controller` / `@RestController`：接收请求
- `@Service`：业务逻辑
- `@Repository`：数据库访问
- `@Component`：普通组件
- `@Configuration`：配置类

例如：

```java
@Service
public class YearlyMemoryService {
}
```

其他类可以通过构造方法接收它：

```java
public YearlyMemoryController(YearlyMemoryService service) {
    this.service = service;
}
```

这叫**依赖注入**。Controller 不需要自己 `new YearlyMemoryService()`，对象的创建和依赖关系由 Spring 管理。

### 3.2 构造器注入

项目普遍使用构造器注入：

```java
private final UserRepository userRepository;

public YearlyMemoryService(UserRepository userRepository) {
    this.userRepository = userRepository;
}
```

优点：

- 依赖明确
- 字段可以使用 `final`
- 更容易编写单元测试
- 避免对象处于“依赖还没有注入”的状态

## 4. Controller、Service、Repository、Entity 分层

项目采用典型的业务分层：

```text
前端请求
   ↓
Controller  接收 HTTP 参数
   ↓
Service     执行业务规则
   ↓
Repository  查询数据库
   ↓
Entity      映射数据库表
```

以生活记录为例：

```text
records/
├─ RecordController.java
├─ RecordService.java
├─ RecordRepository.java
├─ RecordEntity.java
└─ dto/
```

### 4.1 Controller

Controller 只负责接口层工作：

- 读取路径、查询参数和请求体
- 调用 Service
- 返回 Java 对象，让 Spring 自动转换成 JSON

例如 `YearlyMemoryController`：

```java
@RestController
@RequestMapping("/api/ai/yearly-memory")
public class YearlyMemoryController {
    @PostMapping("/query")
    public YearlyMemoryResponse query(
            Authentication authentication,
            @Valid @RequestBody YearlyMemoryQueryRequest request) {
        return service.query(
                authentication.getName(),
                request.year(),
                request.question());
    }
}
```

对应接口：

```text
POST /api/ai/yearly-memory/query
```

### 4.2 Service

Service 是业务逻辑的核心位置，例如：

- 判断用户是否存在
- 判断这一年是否有记录
- 计算记录相关性
- 决定哪些记录可以交给 AI
- 处理状态变化和权限规则

不要把复杂业务全部写在 Controller 中，否则接口层会越来越难维护。

### 4.3 Repository

Repository 负责访问数据库。例如 `RecordRepository` 可以提供按用户和日期范围查询的方法：

```java
findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(...)
```

这是 Spring Data JPA 的派生查询方法。方法名中的单词会被 Spring 解析成查询条件：

- `findAllBy`：查询多条
- `UserId`：按用户 ID
- `AndRecordDateBetween`：日期在两个值之间
- `OrderByRecordDateAsc`：按日期升序

### 4.4 Entity

Entity 表示数据库中的一张表或一条记录。例如 `RecordEntity` 对应生活记录表，通常包含：

- 记录 ID
- 用户 ID
- 日期
- 标题和正文
- 事件类型
- 心情
- 关键词
- 图片对象键
- 创建时间

Entity 适合数据库内部使用，不建议直接作为所有接口的返回值。

## 5. DTO：接口数据对象

DTO 是 Data Transfer Object，用于接口输入和输出。

项目中的 DTO 示例：

```text
user/dto/LoginRequest.java
records/dto/CreateRecordRequest.java
chat/dto/SendChatMessageRequest.java
ai/dto/YearlyMemoryResponse.java
```

DTO 的意义：

1. 防止把数据库 Entity 的全部字段暴露给前端
2. 只接收接口允许的字段
3. 方便使用校验注解
4. 让请求格式和响应格式更加清晰

项目使用 Java `record` 定义简单 DTO：

```java
public record YearlyMemoryResponse(
        String question,
        String answer,
        int year,
        List<MemorySourceResponse> sources
) {
}
```

`record` 会自动生成构造方法、访问方法、`equals`、`hashCode` 和 `toString`。

## 6. HTTP 和 REST API

REST 接口通过 HTTP 方法表达操作：

| 方法 | 常见含义 | 项目示例 |
| --- | --- | --- |
| GET | 查询 | `GET /api/records` |
| POST | 新增或执行动作 | `POST /api/auth/login` |
| PATCH | 部分修改 | `PATCH /api/profile` |
| DELETE | 删除 | `DELETE /api/records/{id}` |

常见 Controller 注解：

```java
@RequestMapping("/api/records")
@GetMapping
@PostMapping
@PatchMapping("/{id}")
@DeleteMapping("/{id}")
```

请求体使用：

```java
@Valid @RequestBody CreateRecordRequest request
```

- `@RequestBody`：把 JSON 请求体转换成 Java 对象
- `@Valid`：触发 DTO 上的参数校验

## 7. 参数校验和异常处理

### 7.1 参数校验

`spring-boot-starter-validation` 支持在 DTO 上声明规则，例如：

- 不能为空
- 长度范围
- 邮箱格式
- 数值范围

当请求不符合规则时，Spring 会抛出校验异常。

### 7.2 全局异常处理

文件位置：

```text
backend/src/main/java/com/earthonline/backend/common/GlobalExceptionHandler.java
```

类上使用：

```java
@RestControllerAdvice
```

它可以统一处理所有 Controller 的异常：

- `400`：参数错误
- `401`：没有登录或 Token 过期
- `403`：没有权限
- `404`：资源不存在
- `409`：业务冲突
- `500`：服务器内部错误

业务代码可以抛出：

```java
throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "这一年暂无记录，无法进行回忆问答");
```

最后由全局处理器转换成统一 JSON，而不是每个 Controller 自己拼错误响应。

## 8. JPA、ORM 和事务

### 8.1 ORM

ORM 是 Object-Relational Mapping，即把 Java 对象映射到关系型数据库表：

```text
Java Entity  ↔  MySQL table
Java field   ↔  table column
Repository   ↔  SQL 查询
```

代码不一定需要手写 SQL，JPA 会根据 Entity 和 Repository 方法完成很多数据库操作。

### 8.2 事务

`YearlyMemoryService` 中：

```java
@Transactional(readOnly = true)
public YearlyMemoryResponse query(...) {
}
```

表示该方法在一个只读事务中运行。

`readOnly = true` 的含义：

- 这个方法主要查询数据
- 不应该修改数据库
- 可以向框架表达只读意图
- 事务边界由 Service 统一控制

需要新增、修改或删除数据的业务方法通常使用普通的 `@Transactional`，保证多个数据库操作要么全部成功，要么全部回滚。

### 8.3 `Optional`

代码：

```java
UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "登录状态无效"));
```

`Optional` 表示“可能找到，也可能找不到”。`orElseThrow` 可以把找不到用户转换成明确的业务异常，避免直接出现空指针异常。

## 9. JWT 登录鉴权

项目使用 JWT 实现无状态认证。

### 9.1 登录流程

```text
用户提交用户名和密码
   ↓
UserController
   ↓
UserService 校验密码
   ↓
JwtService 生成 Token
   ↓
前端保存 accessToken
   ↓
以后请求带 Authorization: Bearer <token>
```

### 9.2 `JwtService`

文件位置：

```text
backend/src/main/java/com/earthonline/backend/auth/JwtService.java
```

生成 Token 时包含：

- subject：用户名
- `userId`：用户 ID
- 签发时间
- 过期时间
- 签名

Token 的作用不是保存所有用户资料，而是证明“这个请求已经通过登录认证”。

### 9.3 `JwtAuthenticationFilter`

文件位置：

```text
backend/src/main/java/com/earthonline/backend/auth/JwtAuthenticationFilter.java
```

它对每个 HTTP 请求执行：

1. 读取 `Authorization` 请求头
2. 提取 `Bearer ` 后的 Token
3. 使用 `JwtService` 验证 Token
4. 从 Token 中取得用户名
5. 写入 `SecurityContextHolder`
6. 继续执行后续过滤器和 Controller

### 9.4 `SecurityContext`

Controller 可以接收：

```java
Authentication authentication
```

然后使用：

```java
authentication.getName()
```

获取当前登录用户名。这样业务代码不需要相信前端传来的 `userId`，而是使用后端认证后的身份。

### 9.5 无状态 Session

`SecurityConfig` 中配置：

```java
SessionCreationPolicy.STATELESS
```

表示服务器不保存传统 Session。每个请求都必须自己携带 JWT，适合前后端分离项目。

## 10. WebSocket、STOMP 和实时聊天

项目聊天同时使用两种通信方式：

- REST：加载历史消息、未读数和已读状态
- WebSocket/STOMP：实时收发消息

### 10.1 WebSocket 地址

```text
/ws/chat
```

前端先连接这个地址，然后使用 STOMP 发送和订阅消息。

### 10.2 `WebSocketConfig`

文件位置：

```text
backend/src/main/java/com/earthonline/backend/config/WebSocketConfig.java
```

主要配置：

```java
registry.enableSimpleBroker("/queue", "/topic");
registry.setApplicationDestinationPrefixes("/app");
registry.setUserDestinationPrefix("/user");
```

含义：

- `/app`：客户端发送给后端 Controller
- `/user`：用户专属目的地
- `/queue`：私人队列
- `/topic`：广播主题

### 10.3 WebSocket Controller

文件位置：

```text
backend/src/main/java/com/earthonline/backend/chat/ChatWebSocketController.java
```

客户端发送：

```text
/app/chat/messages
```

后端接收后保存消息，再推送到：

```text
/user/queue/chat/messages
```

前端只订阅自己的用户队列，所以能收到发给自己的消息。

### 10.4 WebSocket JWT

WebSocket 的 HTTP 握手地址本身可以公开，但 STOMP 的 `CONNECT` 帧必须携带 JWT。

`WebSocketJwtChannelInterceptor` 会：

1. 拦截 `CONNECT`
2. 读取 STOMP 的 `Authorization`
3. 验证 JWT
4. 调用 `accessor.setUser(...)`
5. 把用户名绑定到 WebSocket 会话

如果没有正确设置用户身份，后端的用户专属推送就找不到目标会话。

## 11. MinIO 对象存储

项目不把图片二进制直接保存到 MySQL，而是使用 MinIO。

### 11.1 上传流程

```text
浏览器选择图片
   ↓
前端 POST /api/uploads/...
   ↓
UploadController
   ↓
StorageService
   ↓
MinioClient 上传文件
   ↓
返回 objectKey 或访问地址
   ↓
生活记录只保存 objectKey
```

### 11.2 为什么不直接存数据库

对象存储更适合保存图片、视频和其他文件：

- 数据库表更小
- 文件访问更适合独立存储
- 可以通过对象键管理文件
- 后端可以控制访问地址和权限

`MinioConfig` 负责创建 `MinioClient` Bean，`StorageService` 负责具体的上传业务。

## 12. AI 和 RAG 年度回忆

项目的年度回忆不是把所有记录直接发送给 AI，而是先筛选相关记录，再调用 AI。这是一种简化的 RAG（Retrieval-Augmented Generation，检索增强生成）流程。

### 12.1 代码链路

```text
YearlyMemoryController
   ↓
YearlyMemoryService
   ↓
按用户和年份查询 RecordEntity
   ↓
relevance() 计算相关性
   ↓
选出最多 5 条来源记录
   ↓
YearlyMemoryAiClient
   ↓
调用 DeepSeek
   ↓
返回回答和来源记录
```

### 12.2 用户和年份边界

代码先根据认证用户名查询用户：

```java
UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(...);
```

然后只查询该用户指定年份的记录：

```java
findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
        user.getId(),
        LocalDate.of(year, 1, 1),
        LocalDate.of(year, 12, 31));
```

这一步非常重要：不能把其他用户或其他年份的数据放进 AI 上下文。

### 12.3 相关性计算

`relevance()` 会检查：

- 标题是否匹配
- 事件类型是否匹配
- 心情是否匹配
- 关键词是否匹配
- 内容是否包含查询词

不同字段有不同权重。例如标题和关键词的权重高于正文普通匹配。

### 12.4 中文分词的简化处理

项目没有引入专业中文分词库，而是：

1. 删除“这一年、哪些、事情”等常见虚词
2. 保留单字
3. 生成相邻的两个字作为查询片段
4. 用这些片段和记录字段做字符串匹配

这不是完整的自然语言理解，但实现简单，适合项目规模有限的场景。

### 12.5 Prompt 和上下文

`YearlyMemoryAiClient` 会把筛选后的记录拼成上下文：

```text
日期、标题、类型、心情、关键词、正文
```

系统提示词要求 AI：

- 只能依据提供的个人记录回答
- 不要猜测和虚构
- 记录不足时明确说明
- 控制回答长度
- 不进行医学或心理诊断

这体现了两个重要知识点：

- 上下文构造
- AI 输出边界约束

### 12.6 外部 HTTP 调用

`YearlyMemoryAiClient` 使用 Spring 的 `RestClient` 调用 DeepSeek：

```java
restClient.post()
        .uri("/chat/completions")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(String.class);
```

同时配置：

- 连接超时
- 读取超时
- 429 限流错误
- 网关错误
- 网络访问异常
- JSON 解析失败

## 13. Java 语言知识点

阅读项目代码时会遇到这些 Java 语法：

### 13.1 `final`

```java
private final UserRepository userRepository;
```

表示变量初始化后不能重新指向另一个对象，常用于依赖字段。

### 13.2 泛型

```java
List<RecordEntity>
Map<String, Object>
Optional<UserEntity>
```

泛型规定容器中存放的数据类型，减少类型转换错误。

### 13.3 Lambda

```java
item -> item.record().getRecordDate()
```

Lambda 是把一小段行为作为参数传递，常见于 Stream、排序和集合操作。

### 13.4 Stream

```java
yearlyRecords.stream()
        .map(...)
        .sorted(...)
        .filter(...)
        .limit(...)
        .toList();
```

常见操作：

- `map`：转换每个元素
- `filter`：筛选元素
- `sorted`：排序
- `limit`：限制数量
- `toList`：收集为列表

### 13.5 `record`

```java
private record ScoredRecord(RecordEntity record, double score) {
}
```

适合定义不可变的小型数据结构，例如“记录 + 相关性分数”。

### 13.6 `Set`

```java
Set<String> result = new LinkedHashSet<>();
```

`Set` 不允许重复元素；`LinkedHashSet` 还可以保留插入顺序，适合保存去重后的查询词。

### 13.7 一个 Java 类如何阅读

项目中的类通常可以拆成几个部分：

```java
public class YearlyMemoryService {
   private static final int MAX_SOURCES = 5;

   private final UserRepository userRepository;

   public YearlyMemoryService(UserRepository userRepository) {
      this.userRepository = userRepository;
   }

   public YearlyMemoryResponse query(String username, int year, String question) {
      return ...;
   }
}
```

逐部分理解：

| 代码 | 含义 |
| --- | --- |
| `public class YearlyMemoryService` | 定义一个公开类，类名是 `YearlyMemoryService` |
| `private static final` | 私有、属于类本身、不可重新赋值的常量 |
| `UserRepository userRepository` | 类型是 `UserRepository`，变量名是 `userRepository` |
| `public YearlyMemoryService(...)` | 构造方法，名称必须和类名相同，没有返回类型 |
| `YearlyMemoryResponse query(...)` | 定义方法，返回 `YearlyMemoryResponse` |

Java 方法的基本格式是：

```java
访问修饰符 返回类型 方法名(参数类型 参数名) {
   // 方法体
   return 返回值;
}
```

### 13.8 访问修饰符

项目中常见的访问修饰符：

```java
public class UserController { }
private final JwtService jwtService;
```

- `public`：其他类可以访问
- `private`：只有当前类可以访问
- 没有修饰符：同一个 package 中可以访问
- `protected`：子类或同包类可以访问

通常 Controller 的接口方法是 `public`，Service 的内部辅助方法可以是 `private`，例如：

```java
private String excerpt(String content) {
   ...
}
```

### 13.9 `static` 和实例成员

```java
private static final int MAX_SOURCES = 5;
private final UserRepository userRepository;
```

`static` 成员属于类，不属于某一个对象；`userRepository` 是每个 Service 对象持有的依赖。

使用方式不同：

```java
YearlyMemoryService.MAX_SOURCES; // static 成员通常通过类名访问
service.query(...);              // 实例方法需要对象
```

### 13.10 构造方法和 `this`

```java
private final UserRepository userRepository;

public YearlyMemoryService(UserRepository userRepository) {
   this.userRepository = userRepository;
}
```

等号两边的名字看起来一样：

- 左边 `this.userRepository`：当前对象的字段
- 右边 `userRepository`：构造方法的参数

`this` 表示当前对象。

### 13.11 基本类型、对象类型和数组

项目中会看到：

```java
int year = 2026;
double score = 14.5;
boolean valid = true;
String question = "总结我的成长";
```

- `int`：整数
- `double`：小数
- `boolean`：`true` 或 `false`
- `String`：字符串

数组和集合不是一回事：

```java
String[] names = {"A", "B"};
List<String> nameList = List.of("A", "B");
```

数组长度固定；`List` 是集合，适合保存一组可处理的数据。

### 13.12 `if`、三元表达式和提前返回

普通条件：

```java
if (yearlyRecords.isEmpty()) {
   throw new ResponseStatusException(
         HttpStatus.BAD_REQUEST,
         "暂无记录");
}
```

三元表达式：

```java
String value = content == null ? "" : content.trim();
```

含义是：

```text
条件 ? 条件为真时的值 : 条件为假时的值
```

提前返回可以减少嵌套：

```java
if (apiKey.isBlank()) {
   throw new ResponseStatusException(...);
}
```

发现无法继续时立即结束当前方法，后面的代码就不需要再包在 `else` 中。

### 13.13 `for` 循环和增强 `for`

普通循环：

```java
for (int index = 0; index < records.size(); index++) {
   RecordEntity record = records.get(index);
}
```

增强 `for`：

```java
for (RecordEntity record : records) {
   String content = record.getContent();
}
```

读取集合时，增强 `for` 更容易理解；需要下标时使用普通 `for`。

### 13.14 `Map` 的计数写法

项目中统计关键词时常见：

```java
Map<String, Integer> counts = new HashMap<>();
counts.put(keyword, counts.getOrDefault(keyword, 0) + 1);
```

解释：

1. `Map<String, Integer>` 表示键是字符串，值是整数
2. `getOrDefault(keyword, 0)`：不存在时返回 0
3. `+ 1`：出现次数加一
4. `put`：把新次数写回 Map

### 13.15 方法调用链

项目中经常出现连续调用：

```java
String value = content
      .replaceAll("\\s+", " ")
      .trim();
```

它等价于：

```java
String step1 = content.replaceAll("\\s+", " ");
String value = step1.trim();
```

每个方法返回的对象，作为下一个方法的调用对象。

### 13.16 Lambda 的完整读法

```java
item -> item.record().getRecordDate()
```

可以读成：

> 给我一个叫 `item` 的对象，返回它的记录日期。

排序代码：

```java
.sorted(Comparator
      .comparingDouble(ScoredRecord::score)
      .reversed())
```

其中：

- `ScoredRecord::score` 是方法引用，等价于 `item -> item.score()`
- `comparingDouble` 表示按小数值比较
- `reversed()` 表示从大到小排列

### 13.17 Stream 逐步拆解

代码：

```java
List<RecordEntity> sources = ranked.stream()
      .filter(item -> item.score() > 0)
      .limit(MAX_SOURCES)
      .map(ScoredRecord::record)
      .toList();
```

可以按下面的顺序阅读：

1. `ranked.stream()`：把列表变成 Stream
2. `filter(...)`：只保留分数大于 0 的元素
3. `limit(5)`：最多保留 5 个
4. `map(...)`：从 `ScoredRecord` 取出里面的 `RecordEntity`
5. `toList()`：重新收集为列表

Stream 通常不会修改原来的列表，而是生成处理后的结果。

### 13.18 `Optional` 的三种常见写法

```java
Optional<UserEntity> result = userRepository.findByUsername(username);
```

取值并在不存在时抛异常：

```java
UserEntity user = result.orElseThrow(() -> new RuntimeException("用户不存在"));
```

不存在时提供默认值：

```java
UserEntity user = result.orElse(defaultUser);
```

只在存在时执行：

```java
result.ifPresent(user -> System.out.println(user.getUsername()));
```

### 13.19 异常 `try-catch-finally`

调用外部 AI 时使用：

```java
try {
   String raw = restClient.post()...;
   return parse(raw);
} catch (ResourceAccessException exception) {
   throw new ResponseStatusException(
         HttpStatus.GATEWAY_TIMEOUT,
         "AI 服务响应超时",
         exception);
} catch (Exception exception) {
   throw new ResponseStatusException(
         HttpStatus.BAD_GATEWAY,
         "AI 返回内容无法解析",
         exception);
}
```

- `try`：可能出错的代码
- `catch`：捕获特定异常并转换成业务错误
- `finally`：无论成功还是失败都执行，常用于释放资源
- `throw`：主动抛出异常
- `throws`：声明方法可能向外抛出异常

项目中还会按异常类型分别处理 429、超时和 JSON 解析错误。

### 13.20 注解不是普通方法调用

下面这些写法叫注解：

```java
@Service
@Transactional(readOnly = true)
@PostMapping("/query")
@Valid
```

注解以 `@` 开头，是给 Spring 或 Java 工具读取的元信息：

- `@Service`：把类注册为业务 Bean
- `@Transactional`：声明事务边界
- `@PostMapping`：声明 HTTP 路由
- `@Valid`：开启参数校验

注解本身通常不需要手动调用，它会影响框架运行时行为。

### 13.21 `enum` 枚举

项目中的消息类型和好友状态使用枚举表达有限选项，例如：

```java
public enum ChatMessageType {
   TEXT,
   IMAGE
}
```

枚举比直接使用字符串更安全：

```java
ChatMessageType type = ChatMessageType.TEXT;
```

不会轻易写出拼写错误的 `"TXT"`。

### 13.22 `record` 的访问方式

```java
private record ScoredRecord(RecordEntity record, double score) {
}

ScoredRecord scored = ...;
RecordEntity record = scored.record();
double score = scored.score();
```

注意 `record` 的访问方法写成 `record()`，不是传统 Java Bean 的 `getRecord()`。

### 13.23 Java 字符串文本块

AI 客户端中使用了文本块：

```java
private static final String SYSTEM_PROMPT = """
      你是 Earth Online 的年度回忆助手。
      只能依据提供的个人记录回答。
      """;
```

三个双引号 `"""` 适合保存多行字符串，不需要为每一行手写 `\\n`。

### 13.24 日期 API

项目使用现代 Java 日期类型：

```java
LocalDate start = LocalDate.of(year, 1, 1);
LocalDate end = LocalDate.of(year, 12, 31);
```

`LocalDate` 只表示日期，不包含时间和时区。生活记录的年月日适合使用它。

```java
LocalDate date = LocalDate.now();
LocalDate next = date.plusDays(1);
```

相比旧的 `Date`，`java.time` API 更清晰，也更适合处理日期计算。

### 13.25 Java 代码和 Spring 代码的区别

阅读一个方法时，可以先分辨它属于哪一类：

```java
public YearlyMemoryResponse query(...) {       // Java：方法定义
   List<RecordEntity> records = ...;          // Java：泛型和变量
   records.stream().filter(...).toList();     // Java：Stream
   recordRepository.find...(...);             // 项目：调用 Repository
   throw new ResponseStatusException(...);    // Spring：HTTP 业务异常
}
```

- 类、变量、方法、循环、集合、异常：主要是 Java 语法
- `@Service`、`@PostMapping`、`@Transactional`：主要是 Spring 注解
- `RecordRepository`、`UserEntity`、`YearlyMemoryAiClient`：是本项目的业务类

先把 Java 语法读懂，再理解 Spring 注解如何改变这些 Java 类的运行方式，会更容易。

## 14. 从前端请求到数据库的完整例子

以查询年度回忆为例：

```text
1. YearlyView.vue 调用 queryYearlyMemory()
2. 前端 httpClient 自动添加 JWT
3. POST /api/ai/yearly-memory/query
4. SecurityConfig 判断请求需要认证
5. JwtAuthenticationFilter 验证 Token
6. YearlyMemoryController 接收请求
7. @Valid 校验年份和问题
8. Controller 从 Authentication 取得用户名
9. YearlyMemoryService 查询用户
10. RecordRepository 查询该用户指定年份的记录
11. Service 对记录进行相关性排序
12. YearlyMemoryAiClient 调用 DeepSeek
13. Service 组装回答和来源记录
14. Spring 将 Response DTO 转成 JSON
15. 前端显示 AI 回答和引用记录
```

## 15. 建议的阅读顺序

不要一开始从最复杂的 AI 类读起，建议按以下顺序：

1. `BackendApplication.java`：理解启动入口
2. `application.yaml`：理解外部配置
3. `HealthController.java`：理解最简单的接口
4. `UserController.java` 和 `UserService.java`：理解登录
5. `UserEntity.java` 和 `UserRepository.java`：理解 JPA
6. `RecordController.java` 和 `RecordService.java`：理解 CRUD
7. `GlobalExceptionHandler.java`：理解统一错误处理
8. `SecurityConfig.java` 和 `JwtAuthenticationFilter.java`：理解 JWT
9. `UploadController.java` 和 `StorageService.java`：理解 MinIO
10. `ChatController.java`：理解聊天历史 REST 接口
11. `WebSocketConfig.java` 和 `ChatWebSocketController.java`：理解实时通信
12. `YearlySummaryService.java`：理解 AI 年度总结
13. `YearlyMemoryService.java`：理解检索和排序
14. `YearlyMemoryAiClient.java`：理解外部 AI API 调用

## 16. 常见问题和排查思路

### 后端启动失败

优先检查：

- `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- `JWT_SECRET` 是否存在且长度足够
- MySQL 是否启动
- 端口 `8080` 是否被占用

### 所有接口返回 401

检查：

- 前端是否保存了 Token
- 请求是否携带 `Authorization: Bearer ...`
- `JWT_SECRET` 是否与生成 Token 时一致
- Token 是否已经过期

### 聊天历史能加载但实时消息不更新

检查：

- `/ws/chat` 是否可以连接
- `CHAT_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS` 是否包含当前前端地址
- STOMP `CONNECT` 是否携带 JWT
- `WebSocketJwtChannelInterceptor` 是否执行了 `accessor.setUser(...)`
- 前端是否订阅 `/user/queue/chat/messages`

### 图片上传失败

检查：

- MinIO 是否启动
- endpoint、access-key、secret-key 是否正确
- bucket 是否存在或能否自动创建
- 文件大小是否超过配置限制
- 前端是否上传了正确的 `multipart/form-data`

### AI 回忆失败

检查：

- `DEEPSEEK_API_KEY` 是否配置
- 所选年份是否有记录
- 网络是否能访问 DeepSeek
- 是否触发 429 限流
- 连接和读取超时是否过短

## 17. 常用命令

在 `backend` 目录执行：

```powershell
# 启动后端
./mvnw.cmd spring-boot:run

# 运行测试
./mvnw.cmd test

# 编译并打包
./mvnw.cmd package
```

如果 PowerShell 不接受 `./mvnw.cmd`，也可以使用：

```powershell
mvnw.cmd test
```

## 18. 一句话理解整个后端

这个项目的后端可以理解为：

> Spring Boot Controller 接收请求，Service 编写业务，Repository 访问 MySQL，Entity 映射数据表，DTO 负责接口数据，Security 负责 JWT，MinIO 负责图片，WebSocket 负责实时聊天，AI Client 负责调用 DeepSeek，而 GlobalExceptionHandler 负责把错误统一返回给前端。
