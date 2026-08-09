# Earth Online 后端学习记录

日期：2026-08-07

本文记录今天从创建 Spring Boot 后端，到连接 MySQL、实现时间记录和用户注册登录时遇到的问题，以及对应的基础知识。

## 一、今天完成的功能

当前后端已经完成：

- 创建 Spring Boot Maven 模块 `backend/`
- 配置 Java 21、Spring Boot 和 Maven
- 实现 `GET /api/health`
- 实现时间记录的增删改查基础接口
- 使用 JPA 连接 MySQL
- 在 DataGrip 中创建并查看 `earth_online` 数据库
- 实现用户注册
- 使用 BCrypt 加密密码
- 实现用户名和密码登录
- 登录成功生成 JWT

当前主要结构：

```text
backend/
├─ pom.xml
└─ src/
   ├─ main/
   │  ├─ java/com/earthonline/backend/
   │  │  ├─ BackendApplication.java
   │  │  ├─ config/
   │  │  │  └─ PasswordConfig.java
   │  │  ├─ auth/
   │  │  │  └─ JwtService.java
   │  │  ├─ records/
   │  │  │  ├─ RecordController.java
   │  │  │  ├─ RecordService.java
   │  │  │  ├─ RecordEntity.java
   │  │  │  └─ RecordRepository.java
   │  │  └─ user/
   │  │     ├─ UserController.java
   │  │     ├─ UserService.java
   │  │     ├─ UserEntity.java
   │  │     ├─ UserRepository.java
   │  │     └─ dto/
   │  └─ resources/
   │     └─ application.yaml
   └─ test/
```

## 二、今天遇到的问题和解决方法

### 1. IDEA 中创建模块的位置不正确

目标目录应该是：

```text
E:\Project\Earth-Online\backend
```

在 IDEA 的 Spring Boot 创建窗口中：

```text
名称：backend
位置：E:\Project\Earth-Online
```

IDEA 会自动组合成：

```text
E:\Project\Earth-Online\backend
```

如果位置已经填写到 `backend`，名称又填写 `demo`，就可能产生错误的嵌套目录：

```text
Earth-Online/backend/demo
```

### 2. Maven 依赖没有立即生效

修改 `pom.xml` 后，需要在 IDEA 右侧 Maven 面板点击左上角的圆形刷新按钮：

```text
Reload All Maven Projects
```

以下情况需要刷新 Maven：

- 添加或删除依赖；
- 修改 Spring Boot 或 Java 版本；
- 修改 Maven 插件；
- 切换 Git 分支后 `pom.xml` 发生变化；
- IDEA 报某个依赖包不存在。

只修改 Java 代码、YAML 配置或 Vue 文件时，一般不需要刷新 Maven。

### 3. 浏览器出现 CORS 和 404

前端曾经直接请求：

```text
http://localhost:8080/api
```

这会让浏览器直接访问后端，可能产生 CORS 问题。

本地开发时应让前端请求：

```text
/api
```

再由 Vite 代理到：

```text
http://localhost:8080
```

项目中的 `web/vite.config.js` 已经配置了 `/api` 代理。

同时，Spring Boot 当时还没有实现：

```text
/api/profile
/api/records
/api/stats/dashboard
```

所以会返回 404。开发早期可以先让前端使用 Mock 数据，等后端接口完成后再切换到 API。

### 4. `ERR_CONNECTION_REFUSED`

这个错误表示：

```text
localhost:8080 没有程序正在监听
```

通常是 Spring Boot 没有启动，或者启动后因为错误退出。

检查 IDEA 控制台是否出现：

```text
Started BackendApplication
Tomcat started on port 8080
```

可以使用下面的地址检查后端：

```text
http://localhost:8080/api/health
```

### 5. PowerShell 中文乱码

IDEA 文件底部显示 UTF-8，只代表 Java/YAML 文件使用 UTF-8 保存，不代表 PowerShell 终端也使用 UTF-8。

可以在 PowerShell 中执行：

```powershell
chcp 65001
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new()
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [System.Text.UTF8Encoding]::new()
```

如果浏览器和数据库中的中文正常，只有 PowerShell 显示乱码，通常只是终端显示问题。

### 6. MySQL 连接失败

DataGrip 报：

```text
Connection refused: connect
```

原因是 MySQL Server 没有运行。DataGrip 只是数据库管理工具，不是数据库服务器。

需要在 Windows 服务中启动：

```text
Win + R → services.msc → MySQL → 启动
```

### 7. 数据库不存在

DataGrip 报：

```text
Unknown database 'earth_online'
```

原因是 MySQL 已经运行，但数据库还没有创建。

在 MySQL Console 中执行：

```sql
CREATE DATABASE earth_online
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

`utf8mb4` 可以更好地支持中文和表情符号。

### 8. `application.yaml` 缩进错误

错误写法：

```yaml
spring:
  application:
    name: earth-online-backend

datasource:
  url: ...
```

正确写法：

```yaml
spring:
  application:
    name: earth-online-backend

  datasource:
    url: jdbc:mysql://localhost:3306/earth_online
    username: root
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 86400000

server:
  port: 8080
```

`datasource` 和 `jpa` 必须缩进在 `spring` 下面；`jwt` 和 `server` 与 `spring` 平级。

YAML 对缩进很敏感，建议只使用空格，不要使用 Tab。

### 9. Spring Boot 找不到数据库配置和驱动

错误信息：

```text
Failed to configure a DataSource
'url' attribute is not specified
Failed to determine a suitable driver class
```

需要同时检查：

- `application.yaml` 是否位于 `src/main/resources/`；
- YAML 层级是否正确；
- `pom.xml` 是否包含 JPA 和 MySQL 驱动；
- Maven 是否已经刷新。

相关依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 10. DataGrip 表存在但没有数据

刚开始 `RecordService` 使用的是内存列表：

```java
List<RecordResponse> records;
```

因此网页能显示数据，但 DataGrip 中没有数据。

改成 JPA 后，Service 应该注入：

```java
private final RecordRepository recordRepository;
```

并使用：

```java
recordRepository.save(entity);
recordRepository.findAll();
```

发送 POST 后，可以在 DataGrip 执行：

```sql
SELECT * FROM earth_online.records;
```

重启 Spring Boot 后数据仍然存在，才说明数据已经持久化到数据库。

### 11. 浏览器打开 POST 地址出现 405

浏览器地址栏默认发送 GET 请求。例如打开：

```text
http://localhost:8080/api/auth/login
```

但登录接口定义的是：

```java
@PostMapping("/login")
```

因此会出现：

```text
405 Method Not Allowed
Method 'GET' is not supported
```

GET 可以直接在浏览器地址栏测试；POST、PATCH、DELETE 应使用 PowerShell、Postman 或前端代码发送。

### 12. 缺少 `PasswordEncoder` Bean

错误信息：

```text
UserService required a bean of type PasswordEncoder
```

`UserService` 需要一个 `PasswordEncoder`，但 Spring 不知道如何创建它。

通过 `PasswordConfig` 注册 Bean：

```java
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

文件必须位于根包的子包中：

```text
com.earthonline.backend.config.PasswordConfig
```

这样 `@SpringBootApplication` 才能扫描到它。

### 13. 错误密码没有显示 401

PowerShell 中先创建变量并不会发送请求：

```powershell
$wrongBody = @{ ... } | ConvertTo-Json
```

还需要执行 `Invoke-RestMethod`。

由于 PowerShell 遇到 401 会抛出异常，可以这样查看状态：

```powershell
try {
    Invoke-RestMethod `
        -Uri http://localhost:8080/api/auth/login `
        -Method Post `
        -ContentType "application/json; charset=utf-8" `
        -Body $wrongBody
}
catch {
    $_.Exception.Response.StatusCode
}
```

预期结果是：

```text
Unauthorized
```

## 三、重要基础知识

### 1. Spring Boot 的启动扫描

入口类：

```java
package com.earthonline.backend;

@SpringBootApplication
public class BackendApplication {
}
```

Spring 默认会扫描入口类所在包及其子包。因此业务代码应放在：

```text
com.earthonline.backend
```

下面，例如：

```text
com.earthonline.backend.user
com.earthonline.backend.records
com.earthonline.backend.config
```

### 2. 依赖注入

Controller 不应该自己创建 Service：

```java
new UserService(...)
```

而是通过构造器接收：

```java
public UserController(UserService userService) {
    this.userService = userService;
}
```

`@Service`、`@Repository`、`@Controller` 和 `@Bean` 会让 Spring 管理对象。

### 3. Controller、Service、Repository 的职责

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

- Controller：处理 HTTP 请求和响应；
- Service：编写业务规则；
- Repository：访问数据库；
- Entity：对应数据库表；
- DTO：定义前后端传输的数据格式。

### 4. Entity 和 DTO 的区别

`UserEntity` 对应数据库中的 `users` 表，包含 `passwordHash` 等数据库字段。

`UserResponse` 返回给前端时不能包含密码：

```java
public record UserResponse(
        Long id,
        String username,
        String nickname
) {
}
```

不要直接把 Entity 返回给前端，避免泄露敏感字段。

### 5. BCrypt 密码加密

密码不能以明文保存：

```text
12345678
```

数据库中应该保存类似 BCrypt 的密文：

```text
$2a$10$...
```

注册时：

```java
passwordEncoder.encode(password)
```

登录时：

```java
passwordEncoder.matches(rawPassword, passwordHash)
```

### 6. JWT 是什么

JWT 是登录成功后发给客户端的一段签名字符串。客户端以后请求接口时携带：

```http
Authorization: Bearer <token>
```

服务器通过签名验证 token 是否可信、是否过期，并从中得到用户身份。

JWT 不是密码，也不应该把密码放进 JWT。

### 7. `ddl-auto: update` 的含义

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

开发阶段 Hibernate 会根据 Entity 创建或更新表。

常见取值：

- `none`：不自动处理表；
- `validate`：只检查表结构；
- `update`：开发阶段自动更新；
- `create`：启动时重建表，可能丢数据；
- `create-drop`：启动创建，停止删除。

学习和本地开发可以使用 `update`，生产环境不要依赖它管理数据库结构，应使用 Flyway 等迁移工具。

### 8. Maven 和 npm 的类比

```text
前端 npm                 后端 Maven
package.json             pom.xml
npm install               Maven 下载依赖
node_modules              Maven 本地依赖缓存
npm run build             mvn package
```

修改 `pom.xml` 后刷新 Maven；修改 Java 代码不需要刷新 Maven。

## 四、当前安全提醒

本地学习时可以暂时在 `application.yaml` 中填写数据库密码，但不要把真实密码提交到 GitHub。

之后应改为环境变量：

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
```

并把 `.env`、真实密钥和真实数据库密码加入 `.gitignore`。

## 五、下一步计划

建议继续按这个顺序：

```text
1. 验证登录成功返回 JWT
2. 创建 JWT 请求过滤器
3. 配置 Spring Security
4. 保护 records 接口
5. 实现 /api/auth/me
6. 将 JWT 接入 Vue 前端
7. 实现用户资料接口
8. 完善时间记录的用户归属
```

目前登录只完成了“验证密码并生成 token”，还没有完成“请求携带 token 后识别当前用户”。这正是下一阶段的重点。
## 未来信件模块规划（2026-08-08）

未来信件模块已完成数据库表、用户隔离、增删改查接口，以及前端 mock/API 切换。

当前接口：

```text
GET    /api/future-letters
GET    /api/future-letters/{id}
POST   /api/future-letters
PATCH  /api/future-letters/{id}
DELETE /api/future-letters/{id}
POST   /api/future-letters/{id}/open
```

后续规划：完善开启时间和正文保护、草稿/封存状态、前端编辑删除、到期自动处理、隐私分享规则，以及接口测试。

当前需要真实保存数据时使用：

```env
VITE_FUTURE_LETTERS_DATA_SOURCE=api
```

改为 `mock` 只用于展示页面，不会写入 MySQL。
## 2026-08-08 问题总结：个人资料与未来信件

### 1. 未来信件刷新后消失

原因：页面使用 mock 数据，刷新后不会从数据库读取。解决：增加 `VITE_FUTURE_LETTERS_DATA_SOURCE=api`，让页面通过 `/api/future-letters` 从 MySQL 加载数据。

### 2. 未来信件无法保存到数据库

原因：后端缺少实体、Repository、Service 和 Controller。解决：创建 `future_letters` 表、`FutureLetterEntity`、Repository、DTO、Service 和 Controller，并通过 `user_id` 实现用户隔离。

### 3. 用户资料接口返回 403

原因：IDEA 中仍运行旧进程或旧安全配置。解决：停止旧后端，执行 `Build → Rebuild Project`，重新运行 `BackendApplication`，并重新登录获取新的 JWT。

### 4. 用户资料修改后没有保存

原因：前端原来只保存到 `localStorage`，没有调用后端接口。解决：增加 `GET /api/profile` 和 `PATCH /api/profile`，将头像、邮箱、签名和出生日期保存到 `users` 表。

### 5. nickname、email 字段混用

原因：旧前端使用 nickname，但项目需要独立的邮箱字段。解决：将数据库列从 `nickname` 改为 `email`，并同步修改 Entity、DTO、Service 和 Vue 表单。username 作为登录账号，email 作为邮箱信息。

### 6. 头像上传失败

原因：图片 Base64 内容超过 `VARCHAR(500)` 字段长度。解决：暂时关闭上传，使用 `web/src/assets/picture/avator.jpg` 作为占位图。以后应使用文件存储或对象存储，数据库只保存图片 URL。

### 7. 时间线和年度记录请求失败

原因：后端接口尚未开发，但全局数据源切换到了 API。解决：增加 `fetchMockTimelineData()`，让两个页面暂时使用 mock 数据，其他已完成模块继续使用 API。

### 8. 个人资料页面出现两个用户名

原因：旧 nickname 表单和新的 username/email 表单同时存在。解决：只保留一个只读 username 输入框和一个可编辑 email 输入框。username 是身份标识，后续若要修改，需要同步处理 JWT 和唯一性。

### 今日知识点

- mock 适合展示，API 才能持久化；
- 修改 `.env.local` 后必须重启 Vite；
- 修改 Spring Security 后要停止旧进程并重新构建；
- JWT 过期后需要重新登录；
- 用户身份应从 JWT 获取，不能相信前端传来的 `user_id`；
- 数据库字段、Entity、DTO 和 Vue 表单必须保持一致；
- 图片不适合直接存入普通短字符串字段。
