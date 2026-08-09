# Earth Online：问题与解决记录

> 用途：按发生时间记录项目中的报错、原因、处理方法和验证结果。  
> 规则：遇到问题后先记录“现象”和“报错原文”，修复后补充“原因、解决方法、验证结果”，避免以后重复踩坑。

## 2026-08-09：新闻模块启动失败

### 背景

开始为“地球动态”创建 Spring Boot `news` 模块，新增了：

- `NewsController`；
- `NewsService`；
- `JuheNewsClient`；
- 新闻 DTO；
- 聚合数据 APPKey 配置。

`JuheNewsClient` 的职责是由后端调用聚合数据新闻接口，避免把 APPKey 暴露到 Vue 前端。

---

### 问题 1：找不到 `RestClient.Builder` Bean

#### 现象

启动后端时出现：

```text
Parameter 0 of constructor in com.earthonline.backend.news.JuheNewsClient
required a bean of type 'org.springframework.web.client.RestClient$Builder'
that could not be found.
```

#### 原因

最初的 `JuheNewsClient` 构造函数依赖 Spring 注入：

```java
RestClient.Builder restClientBuilder
```

但当前项目的依赖组合没有自动创建这个 Bean，导致 Spring 在创建 `JuheNewsClient` 时失败，整个应用无法启动。

#### 解决方法

不再注入 `RestClient.Builder`，改为在客户端内部直接创建：

```java
this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build();
```

#### 验证方式

重新构建并启动后端；如果错误不再显示 `RestClient$Builder`，说明这个问题已解决。

---

### 问题 2：找不到 `ObjectMapper` Bean

#### 现象

继续启动时出现：

```text
Parameter 0 of constructor in com.earthonline.backend.news.JuheNewsClient
required a bean of type 'com.fasterxml.jackson.databind.ObjectMapper'
that could not be found.
```

#### 原因

`JuheNewsClient` 的构造函数又要求 Spring 注入 `ObjectMapper`。虽然 Maven 中已经加入 Jackson 相关依赖，Java 类可以被识别，但当前项目没有自动创建可注入的 `ObjectMapper` Bean。

这里需要理解两个概念：

- **依赖存在**：Java 能导入某个类，例如 `ObjectMapper`；
- **Bean 存在**：Spring 容器已经创建该对象，并且可以通过构造函数自动注入。

前者成立不代表后者一定成立。

#### 解决方法

移除构造函数中的 `ObjectMapper` 参数，不再依赖 Spring 注入。改为在 `JuheNewsClient` 内部创建一个可复用的 JSON 解析器：

```java
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
```

请求聚合数据时：

1. 先把 HTTP 响应读取为 `String`；
2. 再调用 `OBJECT_MAPPER.readTree(responseBody)` 转成 `JsonNode`；
3. 从 JSON 中读取 `error_code` 和 `result.data`。

核心代码：

```java
String responseBody = restClient.get()
        .uri(/* 请求参数 */)
        .retrieve()
        .body(String.class);

JsonNode root = OBJECT_MAPPER.readTree(responseBody);
```

#### 验证结果

移除 `ObjectMapper` Bean 注入后，后端已能正常启动。

---

### 本次问题的经验

1. Spring 启动失败时，先看 `Description` 中的 `required a bean of type ...`，它会直接说明缺少什么对象。
2. 不要只因为“类能 import”就认为 Spring 能自动注入它；自动注入要求对应对象已经是 Bean。
3. 如果某个工具对象不需要项目统一配置，且只在一个小模块中使用，可以直接在该模块创建，减少 Bean 配置复杂度。
4. 修改 `pom.xml` 后需要刷新 Maven；只修改 `.java` 文件时通常只需要重新构建或重新启动。
5. 外部 API 的 APPKey 只能放在环境变量或 IDEA 运行配置中，不能写进 Git 或前端代码。

### 当前新闻模块状态

- [x] 后端可以启动；
- [x] `/api/news` 路由已创建；
- [x] 聚合数据客户端代码已创建；
- [x] 配置新的 `JUHE_NEWS_KEY` 并测试真实新闻请求；
- [ ] 增加 10 分钟缓存；
- [ ] 前端切换 API 模式并验证轮播、分类和失败降级。

---

## 2026-08-09：登录成功后调用新闻接口仍返回 403

### 现象

登录接口已经成功返回 `accessToken`，但执行：

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/news?category=all" `
  -Headers @{ Authorization = "Bearer $token" }
```

仍然返回：

```text
403 Forbidden
```

### 原因

PowerShell 变量 `$token` 没有在这一次新的成功登录后重新赋值，新闻请求实际使用的是旧 Token、空值，或之前登录失败时遗留的变量值。

`/api/news` 是受 JWT 保护的接口。没有有效 Token 时，Spring Security 会拒绝访问。

### 解决方法

每次成功登录后，都立即执行：

```powershell
$token = $loginResult.accessToken
```

再检查：

```powershell
$token
$token.Length
```

有效 JWT 应显示为以 `eyJ` 开头的一长串字符，长度通常大于 100。

### 验证结果

重新赋值后，`GET /api/news?category=all` 成功返回 10 条聚合数据新闻。

### 补充：PowerShell 中文乱码

测试输出中的中文显示为乱码，是 Windows PowerShell 控制台编码问题，不代表后端存储或 HTTP JSON 内容本身损坏。浏览器页面正常以 UTF-8 接收 JSON 时应能正确显示中文。
