# MinIO 图片上传排障记录

> 记录日期：2026-08-18  
> 适用项目：Earth Online（Spring Boot + Spring Security + MinIO）

## 目标

实现“用户上传记录图片”功能：

```text
浏览器 / PowerShell
        ↓ 带 JWT 的 multipart/form-data 请求
Spring Boot：POST /api/uploads/images
        ↓ MinIO Java SDK
MinIO：earth-online-records bucket
```

最终结果：图片成功保存到 MinIO 的私有 bucket 中，上传接口返回对象名，例如：

```json
{
  "objectKey": "records/ok123/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.jpg"
}
```

---

## 一、相关配置与代码结构

### 1. MinIO 服务地址

MinIO 有两个常用端口：

| 地址 | 用途 |
| --- | --- |
| `http://localhost:9000` | S3 API 服务端口，Spring Boot 必须连接这里 |
| `http://localhost:9001` | MinIO 浏览器管理后台，供人使用浏览器管理 bucket |

**注意：后端配置不能误用 `9001`。**

### 2. `application.yaml`

MinIO 配置使用环境变量，避免把账号密码提交到 Git：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 45MB

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:}
  secret-key: ${MINIO_SECRET_KEY:}
  bucket: earth-online-records
```

其中：

- 单张图片最大 5MB；
- 一次最多可提交约 9 张图片，所以请求总大小为 45MB；
- `MINIO_ACCESS_KEY` 和 `MINIO_SECRET_KEY` 不写进 YAML 文件；
- YAML 缩进必须使用空格，`servlet` 必须位于 `spring` 内部。

### 3. IDEA 运行环境变量

在 IDEA：`运行 → 编辑配置 → BackendApplication → 环境变量` 中设置：

```text
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=<MinIO 登录用户名>
MINIO_SECRET_KEY=<MinIO 登录密码>
```

这两个凭据必须与创建 MinIO 容器时的 `MINIO_ROOT_USER`、`MINIO_ROOT_PASSWORD` 一致。

修改环境变量后，必须**重启 BackendApplication**，已经运行的 Spring Boot 进程不会自动读取新值。

---

## 二、排障过程

### 问题 1：上传接口返回 403 Forbidden

测试命令：

```powershell
curl.exe -i -X POST "http://localhost:8080/api/uploads/images" `
  -H "Authorization: Bearer $token" `
  -F "file=@C:\图片路径\test.jpg"
```

最初返回：

```text
HTTP/1.1 403
```

#### 第一步：确认 JWT 是否正确

不要猜测 Token 是否失效，直接调用当前用户接口：

```powershell
curl.exe -i "http://localhost:8080/api/auth/me" `
  -H "Authorization: Bearer $token"
```

如果返回 `HTTP/1.1 200`，说明：

- 登录成功；
- `$token` 变量正确；
- JWT 过滤器能够识别该 Token；
- 403 并非普通的“未登录”。

本次测试中 `/api/auth/me` 返回了 200，因此排除 Token 错误。

#### 第二步：检查登录响应字段

登录接口返回的是 `accessToken`，不是 `token`：

```powershell
$token = $response.accessToken
```

错误写法：

```powershell
$token = $response.token
```

错误字段会使 `$token` 为空，上传请求会变成 `Authorization: Bearer `，必然被拒绝。

#### 第三步：让 Spring 显示真正的错误

上传时可能先出现业务异常，随后 Spring 会内部转发到 `/error`。如果 `/error` 又被安全规则拦截，外部只能看到 403，真实原因会被隐藏。

在 `SecurityConfig` 中允许框架的 `ERROR` 内部转发：

```java
.authorizeHttpRequests(auth -> auth
        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
        .requestMatchers(this::isPublicRequest).permitAll()
        .anyRequest().authenticated()
)
```

这不等于公开上传接口。普通 `POST /api/uploads/images` 仍然需要 JWT；仅内部错误分发不再被二次拦截。

完成后，原来的 403 会显示为更有价值的 500 和后端异常日志。

---

### 问题 2：上传接口返回 500 Internal Server Error

出现 500 说明请求已经通过：

```text
JWT 校验 → SecurityConfig → UploadController → StorageService
```

问题进入了 MinIO 操作阶段。

为了不要掩盖根本原因，`StorageService` 需要记录原始异常：

```java
} catch (Exception exception) {
    log.error("MinIO image upload failed", exception);
    throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "图片上传失败，请检查 MinIO 服务和连接配置"
    );
}
```

然后在 IDEA 的 `BackendApplication` 运行控制台中查看 `MinIO image upload failed` 后的异常信息。

#### 如何找到后端运行控制台

不要看 IDEA 的“终端”标签；它只显示 PowerShell。

使用以下任一方式打开 Spring Boot 日志：

- 点击 IDEA 左侧/底部的绿色三角形“运行”图标；
- 菜单：`视图 → 工具窗口 → 运行`；
- 快捷键：`Alt + 4`；
- 在底部选择 `BackendApplication` 标签。

建议测试前先点击控制台的垃圾桶图标清空旧日志，再执行一次上传命令，便于查看本次请求。

---

### 问题 3：MinIO 返回 `SignatureDoesNotMatch`

真实日志关键部分：

```text
ErrorResponseException
code='SignatureDoesNotMatch'
message='The request signature we calculated does not match the signature you provided'
Host: localhost:9000
```

这说明：

- Spring Boot 已连接到正确的 MinIO API 地址 `localhost:9000`；
- bucket 和上传调用已经到达 MinIO；
- 但 Access Key 或 Secret Key 与 MinIO 实际账号不一致。

本次实际原因是：

```text
MINIO_SECRET_KEY 填写错误。
```

#### 解决步骤

1. 停止 `BackendApplication`；
2. IDEA：`运行 → 编辑配置`；
3. 选择 `BackendApplication`；
4. 打开“环境变量”；
5. 将下面两项改为启动 MinIO 时设置的完全一致的值：

```text
MINIO_ACCESS_KEY=<正确的 MinIO 用户名>
MINIO_SECRET_KEY=<正确的 MinIO 密码>
```

6. 检查密码前后没有空格；
7. 确认 `MINIO_ENDPOINT` 仍是 `http://localhost:9000`；
8. 重新启动后端；
9. 重新登录获取新 Token，并再次上传图片。

修复后，图片成功保存进 `earth-online-records` bucket。

---

## 三、推荐测试流程

### 1. 检查后端健康状态

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

### 2. 登录并保存 Token

```powershell
$body = @{
    username = "你的用户名"
    password = "你的密码"
} | ConvertTo-Json

$response = Invoke-RestMethod `
    -Uri "http://localhost:8080/api/auth/login" `
    -Method Post `
    -Body $body `
    -ContentType "application/json"

$token = $response.accessToken
$token.Length
```

Token 长度应为大于 100 的数字。

### 3. 检查 JWT

```powershell
curl.exe -i "http://localhost:8080/api/auth/me" `
  -H "Authorization: Bearer $token"
```

预期：`HTTP/1.1 200`。

### 4. 上传图片

```powershell
curl.exe -i -X POST "http://localhost:8080/api/uploads/images" `
  -H "Authorization: Bearer $token" `
  -F "file=@C:\图片路径\test.jpg"
```

预期：`HTTP/1.1 200`，并返回 `objectKey`。

### 5. 验证 MinIO

打开：

```text
http://localhost:9001
```

进入 bucket：

```text
earth-online-records → records → 用户名
```

刷新后应能看见上传的图片对象。

---

## 四、排障速查表

| 现象 | 优先检查 | 常见原因 |
| --- | --- | --- |
| 后端启动失败 | MySQL、YAML、Maven 依赖 | MySQL 未启动、YAML 缩进错误、依赖未刷新 |
| `/api/auth/me` 返回 403 | Token 获取和 Authorization Header | `$response.token` 写错，应使用 `$response.accessToken`；Token 过期 |
| 上传请求返回 403，但 `/api/auth/me` 返回 200 | 后端日志和 ERROR 分发 | 原始上传异常被 `/error` 的安全规则遮住 |
| 上传请求返回 500 | `StorageService` 后端日志 | MinIO 未启动、端口错误、账号密码错误 |
| `SignatureDoesNotMatch` | IDEA 环境变量 | `MINIO_ACCESS_KEY` 或 `MINIO_SECRET_KEY` 与 MinIO 容器凭据不一致 |
| `Connection refused` | MinIO 是否启动、端口 | MinIO 容器未启动，或后端写成了错误地址 |
| 图片过大 | `spring.servlet.multipart` 配置 | 单张大于 5MB，或 YAML 配置层级错误 |

---

## 五、后续工作

当前已完成“上传图片到 MinIO”。下一步是把上传接口返回的 `objectKey`：

1. 交给前端时间线表单保存；
2. 保存到 `record_photos` 表；
3. 查询记录时由后端生成临时可访问的图片地址；
4. 在首页、时间线和记录详情中展示图片；
5. 删除记录或移除图片时同步删除 MinIO 对象。

> bucket 当前为私有是正确的。不要将永久公开链接直接存入数据库；推荐保存 `objectKey`，每次查询时生成短期访问地址。
