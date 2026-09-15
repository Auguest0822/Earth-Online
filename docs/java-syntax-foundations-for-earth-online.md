# Earth Online 项目 Java 基础语法补强手册

> 本文只关注 Java 语言本身的写法。示例来自 `backend/src/main/java` 中实际出现的代码，再补充必要的基础语法。阅读顺序建议从第 1 节开始，不要一开始跳到 Stream 或 Spring 注解。

## 0. 先建立阅读方法

看到一个 Java 文件，可以按下面顺序阅读：

```text
package / import
        ↓
类、接口或枚举声明
        ↓
字段
        ↓
构造方法
        ↓
公开业务方法
        ↓
private 辅助方法
```

例如：

```java
package com.earthonline.backend.ai;

import java.util.List;

public class ExampleService {
    private final Repository repository;

    public ExampleService(Repository repository) {
        this.repository = repository;
    }

    public List<String> list() {
        return repository.findAll();
    }
}
```

阅读这段代码时，可以先只回答：

1. 这个类叫什么？
2. 它依赖哪些对象？
3. 有哪些公开方法？
4. 方法接收什么参数？
5. 方法返回什么类型？

## 1. Java 文件的基本结构

### 1.1 `package`

```java
package com.earthonline.backend.ai;
```

`package` 表示当前类属于哪个包。包名通常与文件夹路径对应：

```text
com.earthonline.backend.ai
↓
com/earthonline/backend/ai/
```

包的作用：

- 对类进行分类
- 避免不同模块出现同名类冲突
- 配合访问权限控制
- 让 Spring 扫描项目结构更清晰

### 1.2 `import`

```java
import java.time.LocalDate;
import java.util.List;
```

`import` 让当前文件可以直接使用其他包中的类。

没有 `import` 时需要写全名：

```java
java.time.LocalDate date = java.time.LocalDate.now();
```

有了 import 后可以简写：

```java
LocalDate date = LocalDate.now();
```

### 1.3 通配导入

项目配置类中出现过：

```java
import jakarta.persistence.*;
```

`*` 表示导入这个包中的多个类型。初学者可以先理解为“方便使用该包里的注解和类”。大型项目中也常使用明确导入，方便看出依赖来源。

## 2. 类、对象和实例

### 2.1 定义类

```java
public class UserEntity {
}
```

含义：

- `public`：其他包也可以访问
- `class`：声明一个普通类
- `UserEntity`：类名，通常使用大驼峰命名

类是对象的设计图，对象是根据类创建出来的实例。

```java
UserEntity user = new UserEntity();
```

可以拆成：

- `UserEntity`：变量类型
- `user`：变量名
- `new UserEntity()`：创建一个对象
- `=`：把对象引用赋值给变量

### 2.2 对象引用

```java
UserEntity user = null;
```

变量 `user` 的类型是 `UserEntity`，但当前没有指向任何对象。

调用下面的代码可能产生 `NullPointerException`：

```java
user.getUsername();
```

所以项目中经常先查询并判断，或使用 `Optional` 处理可能不存在的结果。

## 3. 字段、变量和常量

### 3.1 字段

```java
private String username;
private LocalDate birthDate;
```

字段是定义在类中的变量，表示对象的状态。

`UserEntity` 中的 `username`、`email`、`birthDate` 都是对象字段。

### 3.2 局部变量

```java
public String normalize(String value) {
    String result = value == null ? "" : value.trim();
    return result;
}
```

`result` 只在这个方法内部有效，叫局部变量。

### 3.3 常量

项目中常见：

```java
private static final int MAX_SOURCES = 5;
private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
```

通常使用全大写加下划线命名。

- `static`：属于类，不属于某个实例
- `final`：初始化后不能再次赋值

常量名一般在类中只定义一次，避免到处出现魔法数字。

### 3.4 `final` 的两种常见理解

```java
final int count = 3;
// count = 4; // 编译错误
```

引用类型中，`final` 不能让对象本身变成不可变，只是不能让变量重新指向另一个对象：

```java
final List<String> names = new ArrayList<>();
names.add("小明");       // 可以修改列表内容
// names = new ArrayList<>(); // 不能重新赋值
```

## 4. 基本类型和引用类型

### 4.1 基本类型

项目中常见：

```java
int year = 2026;
long id = 100L;
double score = 14.5;
boolean enabled = true;
```

| 类型 | 用途 |
| --- | --- |
| `int` | 普通整数 |
| `long` | 较大的整数，项目中的数据库 ID 常用 `Long` |
| `double` | 小数和评分 |
| `boolean` | 真或假 |
| `char` | 单个字符 |

### 4.2 包装类型

```java
Long id;
Integer count;
Boolean enabled;
Double score;
```

包装类型是对象形式的基本类型。项目的 Entity 和 DTO 中经常使用它们，因为它们可以表示 `null`：

```java
Integer year = null;
```

`int` 不能为 `null`，`Integer` 可以为 `null`。

### 4.3 字符串

```java
String question = "总结我的成长";
```

常见操作：

```java
question.trim();              // 去掉首尾空白
question.isBlank();            // 是否为空或只包含空白
question.length();             // 长度
question.contains("成长");    // 是否包含字符串
question.toLowerCase();        // 转小写
question.replace("旧", "新"); // 替换
```

Java 的 `String` 是不可变对象。调用 `trim()` 不会修改原字符串，而是返回一个新字符串：

```java
String clean = question.trim();
```

## 5. 访问修饰符和封装

### 5.1 四种常见访问级别

```java
public    // 任何地方都可以访问
protected // 子类和同包类可以访问
// 无修饰符 // 同包可以访问
private   // 只有当前类可以访问
```

项目中常见结构：

```java
public class UserService {
    private final UserRepository userRepository;

    public UserResponse getCurrentUser(String username) {
        return ...;
    }

    private UserEntity findUser(String username) {
        return ...;
    }
}
```

外部只调用公开方法，内部查询细节通过 `private` 隐藏，这就是封装。

### 5.2 Getter 和 Setter

Entity 中常见：

```java
public String getUsername() {
    return username;
}

public void setSlogan(String slogan) {
    this.slogan = slogan;
}
```

- Getter：读取字段
- Setter：修改字段
- `this.slogan`：当前对象的字段
- `slogan`：方法参数

方法命名遵循 Java Bean 约定，Jackson、JPA 等框架会识别这种写法。

## 6. 方法、参数和返回值

### 6.1 方法基本形式

```java
public String excerpt(String content) {
    return content.substring(0, 20);
}
```

结构：

```text
public       访问修饰符
String       返回类型
excerpt      方法名
String content 参数
return       返回结果
```

### 6.2 无返回值方法

```java
public void setEmail(String email) {
    this.email = email;
}
```

`void` 表示方法不返回结果。

### 6.3 多个参数

```java
public YearlyMemoryResponse query(
        String username,
        int year,
        String rawQuestion) {
    ...
}
```

每个参数都必须声明类型，参数之间使用逗号分隔。

### 6.4 方法重载

同一个类中可以有同名但参数列表不同的方法：

```java
void log(String message) { }
void log(String message, Throwable error) { }
```

返回类型不同不能单独构成重载，参数必须不同。

### 6.5 `return`

```java
if (records.isEmpty()) {
    return List.of();
}
return records;
```

`return` 有两个作用：

1. 立即结束当前方法
2. 返回一个值（如果方法有返回类型）

## 7. 构造方法和对象初始化

### 7.1 无参构造方法

JPA Entity 通常需要无参构造器：

```java
public UserEntity() {
}
```

### 7.2 有参构造方法

```java
public UserEntity(
        String username,
        String passwordHash,
        String email,
        LocalDateTime createdAt) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.email = email;
    this.createdAt = createdAt;
}
```

创建对象时：

```java
UserEntity user = new UserEntity(
        username,
        passwordHash,
        email,
        LocalDateTime.now());
```

### 7.3 `this`

`this` 表示当前对象：

```java
this.email = email;
```

当字段名和参数名相同时，需要用 `this` 区分两者。

## 8. 条件判断

### 8.1 `if / else`

```java
if (apiKey.isBlank()) {
    throw new ResponseStatusException(...);
} else {
    callAi();
}
```

如果只有一种失败情况，项目常使用提前抛异常的写法，省略 `else`：

```java
if (apiKey.isBlank()) {
    throw new ResponseStatusException(...);
}

callAi();
```

### 8.2 比较运算

```java
count > 0
count >= 3
status == FriendRequestStatus.ACCEPTED
name.equals(otherName)
```

注意：

- 基本类型通常使用 `==`
- 对象内容比较应使用 `.equals()`
- 字符串不要使用 `==` 比较内容

错误：

```java
if (username == "admin") { }
```

正确：

```java
if ("admin".equals(username)) { }
```

这样即使 `username` 是 `null` 也不会因为调用对象为空而报错。

### 8.3 逻辑运算

```java
if (user != null && user.getId() != null) {
    ...
}
```

- `&&`：并且，左边为假时右边不再计算
- `||`：或者，左边为真时右边不再计算
- `!`：取反

这种“提前停止”叫短路求值。

### 8.4 三元表达式

```java
String title = value == null ? "未命名" : value;
```

格式：

```java
条件 ? 条件为真时的结果 : 条件为假时的结果
```

适合简单赋值，不适合写很长的业务逻辑。

### 8.5 `switch` 表达式

项目异常处理代码中出现：

```java
return switch (status) {
    case 400 -> "请求参数不正确";
    case 401 -> "登录状态已过期，请重新登录";
    case 404 -> "请求的内容不存在";
    default -> "请求失败，请稍后重试";
};
```

这是现代 Java 的 `switch` 表达式，可以直接产生返回值。`->` 分支不需要写 `break`。

## 9. 循环

### 9.1 普通 `for`

```java
for (int index = 0; index < records.size(); index++) {
    RecordEntity record = records.get(index);
}
```

三部分：

1. 初始化：`int index = 0`
2. 条件：`index < records.size()`
3. 每轮变化：`index++`

### 9.2 增强 `for`

```java
for (RecordEntity record : records) {
    System.out.println(record.getContent());
}
```

读作“从 records 中依次取出一个 RecordEntity，命名为 record”。

### 9.3 `while`

```java
while (size > 0) {
    size--;
}
```

当条件为真时重复执行。项目中处理文件、重试或未知次数循环时可能使用它。

### 9.4 `break` 和 `continue`

```java
for (String value : values) {
    if (value == null) continue;
    if (value.equals("stop")) break;
}
```

- `continue`：跳过当前轮，进入下一轮
- `break`：结束整个循环

## 10. 数组和集合

### 10.1 数组

```java
String[] patterns = {"http://localhost:5173", "http://127.0.0.1:5173"};
```

数组长度固定：

```java
patterns.length
```

### 10.2 `List`

```java
List<String> names = new ArrayList<>();
names.add("小明");
names.add("小红");
String first = names.get(0);
```

`List` 有顺序，允许重复。

常用方法：

```java
list.size();
list.isEmpty();
list.get(index);
list.add(value);
list.remove(value);
list.contains(value);
```

### 10.3 `Set`

```java
Set<String> fillers = Set.of("这一年", "今年", "哪些");
```

`Set` 不允许重复。适合保存唯一值，例如：

- 允许的文件类型
- 去重后的关键词
- 查询中的词语

### 10.4 `Map`

```java
Map<String, Integer> counts = new HashMap<>();
counts.put("学习", 1);
int count = counts.getOrDefault("学习", 0);
```

`Map` 保存键值对：

```text
key   → value
学习  → 1
旅行  → 3
```

项目中的计数写法：

```java
counts.put(keyword, counts.getOrDefault(keyword, 0) + 1);
```

### 10.5 集合工厂方法

```java
List<String> list = List.of("A", "B");
Set<String> set = Set.of("A", "B");
Map<String, Object> map = Map.of("model", model, "stream", false);
```

这些方法适合快速创建小型集合。它们创建的集合通常不可修改：

```java
List<String> list = List.of("A");
// list.add("B"); // 运行时会抛 UnsupportedOperationException
```

如果需要修改，创建可变集合：

```java
List<String> list = new ArrayList<>(List.of("A"));
list.add("B");
```

## 11. 泛型

### 11.1 泛型基本形式

```java
List<RecordEntity>
Optional<UserEntity>
Map<String, Object>
```

尖括号表示集合内部的数据类型：

- `List<RecordEntity>`：记录列表
- `Optional<UserEntity>`：可能存在的用户
- `Map<String, Object>`：字符串键、任意对象值

### 11.2 为什么需要泛型

没有泛型时：

```java
List values = new ArrayList();
values.add("text");
String value = (String) values.get(0);
```

有泛型后：

```java
List<String> values = new ArrayList<>();
values.add("text");
String value = values.get(0);
```

编译器可以提前检查类型，减少强制类型转换。

### 11.3 `<>` 菱形语法

```java
List<String> values = new ArrayList<>();
```

右侧的 `<>` 省略了重复的 `String`，编译器可以从左边推断类型。

### 11.4 `var`

项目中有：

```java
var result = recordRepository.findPageByFilters(...);
```

`var` 不是动态类型。编译器会在编译时推断真实类型，变量类型之后不能改变。

它适合右侧类型很长时使用，但初学者阅读时可以先把它替换成具体类型理解。

## 12. `Optional`

### 12.1 为什么使用 Optional

Repository 查询单个用户时可能找不到：

```java
Optional<UserEntity> result = userRepository.findByUsername(username);
```

它明确表示“结果可能为空”，比直接返回 `null` 更容易被注意到。

### 12.2 `orElseThrow`

项目常见写法：

```java
UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "登录状态无效"));
```

含义：找到用户就取出；找不到就执行 Lambda 抛异常。

### 12.3 `orElse` 和 `orElseGet`

```java
UserEntity user = optional.orElse(defaultUser);
```

没有值时使用已经准备好的 `defaultUser`。

```java
UserEntity user = optional.orElseGet(() -> createDefaultUser());
```

没有值时才调用 `createDefaultUser()`。

### 12.4 `map`

```java
String username = optionalUser
        .map(UserEntity::getUsername)
        .orElse("");
```

如果用户存在，就把用户转换为用户名；不存在就返回空字符串。

## 13. 异常处理

### 13.1 异常是什么

异常表示程序运行中出现了不能正常继续处理的情况，例如：

- 用户不存在
- 文件类型不允许
- AI 请求超时
- JSON 解析失败
- 数据库连接失败

### 13.2 `throw`

```java
throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "好友不存在");
```

`throw` 立即抛出一个异常，当前方法停止执行。

### 13.3 `try-catch`

```java
try {
    String raw = callRemoteService();
    return parse(raw);
} catch (ResourceAccessException exception) {
    throw new ResponseStatusException(
            HttpStatus.GATEWAY_TIMEOUT,
            "服务响应超时",
            exception);
}
```

`try` 中放可能出错的代码，`catch` 捕获指定类型的异常。

### 13.4 多个 `catch`

```java
try {
    ...
} catch (RestClientResponseException exception) {
    ...
} catch (ResourceAccessException exception) {
    ...
} catch (Exception exception) {
    ...
}
```

越具体的异常通常越应该放前面，`Exception` 这种范围很大的异常放最后。

### 13.5 `finally`

```java
try {
    openResource();
} finally {
    closeResource();
}
```

无论成功还是失败，`finally` 都会执行。现代 Java 处理文件和网络资源时还常用 `try-with-resources`。

### 13.6 `catch` 中重新抛出

项目会把底层异常转换成前端能理解的 HTTP 异常：

```java
catch (ResourceAccessException exception) {
    throw new ResponseStatusException(
            HttpStatus.GATEWAY_TIMEOUT,
            "AI 服务响应超时，请稍后重试",
            exception);
}
```

第三个参数保留原始异常，方便日志排查。

## 14. Lambda 表达式和方法引用

### 14.1 Lambda 基本形式

```java
name -> name.trim()
```

读作：“接收一个 name，返回 `name.trim()`”。

多个参数：

```java
(a, b) -> a + b
```

多条语句：

```java
item -> {
    String value = item.trim();
    return value.toLowerCase();
}
```

### 14.2 常见项目例子

```java
.filter(item -> item.score() > 0)
```

只保留分数大于 0 的元素。

```java
.map(record -> new ScoredRecord(
        record,
        relevance(question, record)))
```

把每条记录转换成一个 `ScoredRecord`。

```java
.forEach(sources::add)
```

对每个元素调用 `sources.add(element)`。

### 14.3 方法引用 `::`

```java
UserEntity::getId
```

等价于：

```java
user -> user.getId()
```

```java
String::trim
```

等价于：

```java
value -> value.trim()
```

方法引用只是 Lambda 的简写形式。

## 15. Stream 流式处理

### 15.1 基本链式结构

```java
List<RecordEntity> sources = ranked.stream()
        .filter(item -> item.score() > 0)
        .limit(MAX_SOURCES)
        .map(ScoredRecord::record)
        .toList();
```

从上到下阅读：

1. 把 `ranked` 转成 Stream
2. 筛选分数大于 0 的项目
3. 最多保留 `MAX_SOURCES` 条
4. 取出每个项目中的 `record`
5. 收集为 List

### 15.2 `map`

`map` 改变元素类型或内容：

```java
List<String> names = users.stream()
        .map(UserEntity::getUsername)
        .toList();
```

输入是用户，输出是用户名。

### 15.3 `filter`

```java
List<UserEntity> activeUsers = users.stream()
        .filter(user -> user.isActive())
        .toList();
```

只保留满足条件的元素。

### 15.4 `sorted`

```java
records.stream()
        .sorted(Comparator.comparing(RecordEntity::getRecordDate))
        .toList();
```

按照记录日期排序。

倒序：

```java
.sorted(Comparator
        .comparing(RecordEntity::getRecordDate)
        .reversed())
```

### 15.5 `limit`、`distinct`、`count`

```java
stream.limit(5);     // 最多取 5 个
stream.distinct();   // 去重
stream.count();      // 统计数量
```

### 15.6 `anyMatch` 和 `allMatch`

```java
boolean exists = requests.stream()
        .anyMatch(request -> request.getStatus() == ACCEPTED);
```

只要有一个元素满足条件，就返回 `true`。

```java
boolean valid = values.stream()
        .allMatch(value -> !value.isBlank());
```

所有元素都满足条件才返回 `true`。

### 15.7 Stream 不等于数据库查询

```java
recordRepository.findAllByUserId(userId)
        .stream()
        .map(this::toResponse)
        .toList();
```

这里先由 Repository 从数据库取出数据，然后 Stream 在 Java 内存中转换结果。不要把 Stream 误认为 SQL，它们处于不同层次。

## 16. `record` 类型

### 16.1 定义 DTO

项目中大量 DTO 使用：

```java
public record LoginRequest(
        String username,
        String password) {
}
```

它相当于一个主要用于保存数据的不可变类。

### 16.2 访问字段

```java
request.username()
request.password()
```

`record` 的访问方法没有 `get` 前缀。

### 16.3 和普通类的区别

普通类：

```java
user.getUsername()
```

Record：

```java
request.username()
```

Record 默认不提供 Setter，适合请求对象和响应对象。

### 16.4 内部 record

`YearlyMemoryService` 中：

```java
private record ScoredRecord(RecordEntity record, double score) {
}
```

这是只在当前类内部使用的小型数据结构，表示：

```text
一条记录 + 它的相关性分数
```

## 17. `enum` 枚举

项目中有消息类型、好友关系状态和好友申请状态等枚举：

```java
public enum ChatMessageType {
    TEXT,
    IMAGE
}
```

使用：

```java
ChatMessageType type = ChatMessageType.TEXT;
```

比较：

```java
if (type == ChatMessageType.IMAGE) {
    ...
}
```

枚举适合表示有限且固定的选项，比到处使用字符串更安全。

## 18. 接口、实现和继承

### 18.1 接口

```java
public interface YearlySummaryAiClient {
    DeepSeekYearlySummary generate(...);
}
```

接口定义“必须具备什么能力”，通常不负责具体实现。

### 18.2 `implements`

```java
public class DeepSeekYearlySummaryClient
        implements YearlySummaryAiClient {
}
```

表示这个类实现了接口，必须提供接口声明的方法。

接口让业务层依赖抽象，而不是依赖某个具体的 AI 实现，便于替换实现和测试。

### 18.3 `extends`

认证过滤器：

```java
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {
}
```

`extends` 表示继承父类，可以复用父类能力，并重写父类方法。

### 18.4 `@Override`

```java
@Override
protected void doFilterInternal(...) {
    ...
}
```

表示这个方法是重写父类或实现接口的方法。编译器会帮你检查方法名和参数是否正确。

## 19. 注解语法

注解以 `@` 开头，是附加在类、方法、字段或参数上的元信息。

### 19.1 类注解

```java
@Service
public class RecordService { }
```

```java
@RestController
@RequestMapping("/api/records")
public class RecordController { }
```

### 19.2 方法注解

```java
@GetMapping
public List<RecordResponse> list() { }
```

```java
@Transactional(readOnly = true)
public YearlyMemoryResponse query(...) { }
```

### 19.3 字段注解

Entity 中：

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

这不是普通 Java 赋值语句，而是告诉 JPA 如何映射字段。

### 19.4 参数注解

```java
public UserResponse login(
        @Valid @RequestBody LoginRequest request) {
}
```

- `@RequestBody`：从 HTTP JSON 请求体读取对象
- `@Valid`：启用参数校验

注解本身仍然是 Java 语法的一部分，但具体效果由 Spring、JPA 等框架解释。

## 20. 实体类中的 JPA 语法

用户实体中：

```java
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

从 Java 语法角度看，这只是：

- 一个类
- 一个字段
- 字段类型是 `Long`
- 字段前面有注解

从 JPA 角度看，注解表示：

- `@Entity`：这个类对应数据库实体
- `@Table`：对应 `users` 表
- `@Id`：主键
- `@GeneratedValue`：数据库自动生成 ID

关联字段示例：

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
private UserEntity sender;
```

Java 角度：字段类型是 `UserEntity`。

JPA 角度：多个消息可以属于一个发送者用户。

## 21. 日期和时间语法

### 21.1 `LocalDate`

```java
LocalDate date = LocalDate.now();
LocalDate start = LocalDate.of(year, 1, 1);
LocalDate end = LocalDate.of(year, 12, 31);
```

`LocalDate` 只保存年月日，适合生活记录日期和生日。

### 21.2 `LocalDateTime`

```java
LocalDateTime createdAt = LocalDateTime.now();
```

保存日期和时间，适合：

- 创建时间
- 更新时间
- 聊天消息时间
- 未来信件开启时间

### 21.3 日期比较

```java
if (LocalDateTime.now().isBefore(entity.getOpenAt())) {
    ...
}
```

不要用字符串比较日期，应使用日期对象提供的比较方法。

### 21.4 日期范围

```java
LocalDate.of(year, 1, 1)
LocalDate.of(year, 12, 31)
```

用于构造一年的起止日期，再交给 Repository 查询。

## 22. 字符串文本块和格式化

### 22.1 文本块

AI 客户端使用：

```java
private static final String SYSTEM_PROMPT = """
        你是 Earth Online 的年度回忆助手。
        只能依据提供的个人记录回答。
        """;
```

三个双引号适合编写多行字符串。

### 22.2 `formatted`

```java
return """
        用户正在回顾 %d 年，问题是：%s
        """.formatted(year, question);
```

`%d` 对应整数，`%s` 对应字符串。

效果类似：

```java
String text = String.format("用户正在回顾 %d 年", year);
```

## 23. `StringBuilder`

项目构造 AI 上下文时使用：

```java
StringBuilder context = new StringBuilder();
context.append("日期：");
context.append(record.getRecordDate());
context.append("；正文：");
context.append(record.getContent());
context.append('\n');
```

`StringBuilder` 适合在循环中不断拼接字符串，避免大量创建临时 String 对象。

最后得到字符串：

```java
String prompt = context.toString();
```

## 24. `UUID`、随机数和类型转换

### 24.1 UUID

文件存储中：

```java
String objectKey = UUID.randomUUID() + extension;
```

`UUID.randomUUID()` 生成随机唯一标识，适合避免上传文件重名。

### 24.2 数值转换

```java
int value = Integer.parseInt(text);
long id = Long.parseLong(text);
```

字符串转换失败时会抛出 `NumberFormatException`。

### 24.3 类型转换

```java
Object value = "text";
String text = (String) value;
```

括号是强制类型转换。转换前必须确保对象真实类型匹配，否则会抛 `ClassCastException`。

## 25. `null`、空字符串和空集合

这三个概念不同：

```java
String a = null;       // 没有对象
String b = "";        // 有字符串对象，但长度为 0
List<String> c = List.of(); // 有列表对象，但列表没有元素
```

项目常见判断：

```java
if (value == null) { }
if (value.isBlank()) { }
if (list.isEmpty()) { }
```

不要对可能为 `null` 的对象直接调用方法：

```java
value.trim(); // value 为 null 时会报错
```

可以写成：

```java
String clean = value == null ? "" : value.trim();
```

## 26. 赋值、引用和可变对象

```java
List<String> first = new ArrayList<>();
List<String> second = first;
second.add("A");
```

`first` 和 `second` 指向同一个列表，所以 `first` 也能看到 `A`。

如果想复制列表：

```java
List<String> copy = new ArrayList<>(first);
```

项目中常见的复制写法：

```java
List<RecordEntity> sorted = new ArrayList<>(records);
```

这样排序 `sorted` 不会直接改变原始列表。

## 27. 访问器、方法引用和 `this`

下面三种写法含义相近：

```java
.map(record -> record.getId())
.map(RecordEntity::getId)
.map(this::toResponse)
```

- 第一种：Lambda，明确写参数
- 第二种：引用某个类的实例方法
- 第三种：引用当前对象的实例方法

`this::toResponse` 可以理解成：

```java
entity -> this.toResponse(entity)
```

## 28. 接口类型和实现类型

```java
List<String> names = new ArrayList<>();
```

左边使用接口类型 `List`，右边使用具体实现 `ArrayList`。

这样做的好处是：调用方只依赖 List 的能力，未来可以替换实现：

```java
List<String> names = new LinkedList<>();
```

只要后续代码使用的是 List 支持的方法，就不需要修改其他逻辑。

## 29. 项目中最常见的复杂代码拆解

### 29.1 Repository 查询加 Optional

```java
UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "登录状态无效"));
```

逐步翻译：

```text
调用 userRepository 的 findByUsername
得到 Optional<UserEntity>
如果存在，取出 UserEntity
如果不存在，执行 Lambda
Lambda 创建并抛出 ResponseStatusException
```

### 29.2 排序和方法引用

```java
.sorted(Comparator.comparingDouble(ScoredRecord::score).reversed()
        .thenComparing(item -> item.record().getRecordDate(),
                Comparator.reverseOrder()))
```

逐步翻译：

```text
先按 score 从高到低排序
如果 score 相同，再按记录日期从新到旧排序
```

### 29.3 `Map` 统计次数

```java
counts.set(name, (counts.getOrDefault(name, 0) + 1));
```

逐步翻译：

```text
取出 name 当前次数
如果没有则使用 0
加 1
写回 Map
```

### 29.4 Stream 构造响应

```java
return entities.stream()
        .map(entity -> toResponse(entity, username))
        .toList();
```

逐步翻译：

```text
拿到实体列表
逐条转换成响应对象
收集成响应列表并返回
```

## 30. 语法学习顺序

建议按这个顺序补基础：

### 第一阶段：能看懂类

1. `package` 和 `import`
2. 类和对象
3. 字段和局部变量
4. 基本类型和 `String`
5. `public`、`private`、`final`
6. 构造方法和 `this`
7. 方法参数和返回值

### 第二阶段：能看懂业务条件

1. `if / else`
2. 比较运算
3. `&&`、`||`、`!`
4. 三元表达式
5. `for`、增强 `for`
6. `switch`
7. `throw` 和 `try-catch`

### 第三阶段：能看懂数据处理

1. `List`
2. `Set`
3. `Map`
4. 泛型
5. `Optional`
6. `record`
7. `enum`

### 第四阶段：能看懂项目高级写法

1. Lambda
2. 方法引用 `::`
3. Stream
4. 接口和 `implements`
5. 继承和 `extends`
6. 注解
7. 日期 API
8. `StringBuilder`、文本块和 UUID

## 31. 自测练习

### 练习 1：读取方法签名

解释下面代码：

```java
public List<RecordResponse> list(String username) {
}
```

参考答案：

- `public`：其他类可以调用
- `List<RecordResponse>`：返回记录响应列表
- `list`：方法名
- `String username`：接收一个字符串参数

### 练习 2：读懂三元表达式

```java
LocalDate date = request.date() == null
        ? LocalDate.now()
        : request.date();
```

含义：请求没有日期时使用今天，否则使用请求中的日期。

### 练习 3：读懂 Stream

```java
List<String> names = users.stream()
        .filter(user -> user.getAvatar() != null)
        .map(UserEntity::getUsername)
        .toList();
```

含义：从用户列表中筛选有头像的用户，取出用户名，收集成列表。

### 练习 4：读懂 Optional

```java
String publicId = userRepository.findById(id)
        .map(UserEntity::getPublicId)
        .orElse("未生成");
```

含义：找到用户就取 publicId，找不到或 publicId 为空结果链断开时使用“未生成”。

## 32. 最后记忆表

| 语法 | 看到它时先想到 |
| --- | --- |
| `class` | 普通类 |
| `interface` | 能力约定 |
| `enum` | 固定选项 |
| `record` | 简单不可变数据对象 |
| `extends` | 继承父类 |
| `implements` | 实现接口 |
| `new` | 创建对象 |
| `this` | 当前对象 |
| `static final` | 类级常量 |
| `List<T>` | 有序集合 |
| `Set<T>` | 不重复集合 |
| `Map<K,V>` | 键值对 |
| `Optional<T>` | 可能没有值 |
| `->` | Lambda |
| `::` | 方法引用 |
| `.stream()` | 开始流式处理 |
| `? :` | 三元表达式 |
| `throw` | 主动抛异常 |
| `@` | 注解或框架元信息 |
| `LocalDate` | 只有日期 |
| `LocalDateTime` | 日期加时间 |

掌握这些语法后，再回头阅读 `YearlyMemoryService.java`，可以把它拆成：类和字段、构造器注入、查询方法、异常、集合、Stream 排序、私有辅助方法和内部 `record`，不会再把整段代码看成一个整体。
