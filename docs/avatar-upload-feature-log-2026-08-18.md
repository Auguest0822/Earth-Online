# 2026-08-18：头像上传功能完成记录

> 范围：本文件只记录本次在 MinIO 图片上传基础已可用之后，完成“用户头像上传、保存与展示”的工作。MinIO 的安装、Bucket 创建、图片上传排错过程请查看已有的 `Docker_Desktop_MinIO.md` 与 `minio-image-upload-troubleshooting.md`。

## 1. 本次目标

让用户可以在“我的地球档案”编辑个人资料时选择头像，并完成下面的完整流程：

1. 前端选择 JPG、PNG 或 WebP 图片。
2. 图片上传到 MinIO 私有 Bucket。
3. MySQL `users.avatar` 只保存 MinIO 对象键（不是临时访问链接）。
4. 后端读取个人资料时，生成可临时访问的签名 URL。
5. 个人资料页、顶部导航栏都显示新头像。

## 2. 设计决定

### 2.1 头像文件保存位置

头像与记录图片分开保存，MinIO 中的路径格式为：

```text
avatars/<用户名>/<UUID>.<文件后缀>
```

例如：

```text
avatars/ok123/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.jpg
```

这样可以避免不同用户同名文件互相覆盖，也方便之后按用户管理头像。

### 2.2 数据库保存什么

`users.avatar` 保存的是对象键，例如：

```text
avatars/ok123/xxxxxxxx.jpg
```

不直接保存签名 URL，原因是签名 URL 会过期。每次登录、刷新资料时，后端都会根据对象键生成新的临时访问地址。

## 3. 后端改动

### 3.1 存储服务

文件：`backend/src/main/java/com/earthonline/backend/storage/StorageService.java`

- 抽取通用图片上传逻辑。
- 原有记录图片使用 `records/<用户名>/...`。
- 新增头像上传逻辑，使用 `avatars/<用户名>/...`。
- 限制头像格式为 JPEG、PNG、WebP，单张不超过 5MB。
- 新增头像预览地址生成逻辑：只允许读取属于当前用户名 `avatars/<用户名>/` 路径下的对象键。

### 3.2 头像上传接口

文件：`backend/src/main/java/com/earthonline/backend/storage/UploadController.java`

新增接口：

```text
POST /api/uploads/avatar
```

请求方式：`multipart/form-data`，文件字段名为 `file`，必须携带 JWT。

返回示例：

```json
{
  "objectKey": "avatars/ok123/xxxxxxxx.jpg",
  "previewUrl": "http://localhost:9000/...临时签名参数..."
}
```

前端提交个人资料更新时，把 `objectKey` 作为 `avatar` 字段保存即可。

### 3.3 用户资料响应

文件：

- `backend/src/main/java/com/earthonline/backend/user/UserService.java`
- `backend/src/main/java/com/earthonline/backend/user/ProfileService.java`

返回用户资料时：

- 如果 `avatar` 是新的 MinIO 对象键，转换为签名 URL 返回给前端。
- 如果 `avatar` 是历史遗留的 `http://` 或 `https://` 地址，暂时原样返回，保持兼容。

## 4. 前端改动

### 4.1 上传 API

文件：`web/src/api/modules/uploads.js`

新增 `uploadAvatar(file)`：使用 `FormData` 调用 `/api/uploads/avatar`。

### 4.2 个人资料页

文件：`web/src/views/PersonalView.vue`

- 编辑资料弹窗增加“选择头像”按钮。
- 选择文件后先使用浏览器本地预览。
- 点击“保存”时：先上传头像，再调用个人资料更新接口。
- 页面头像优先使用接口返回的头像 URL；没有头像时使用项目内置占位图。

### 4.3 顶部导航栏

文件：`web/src/components/Header/Header.vue`

顶部头像改为读取当前用户资料中的 `avatar`，没有头像时显示默认头像。

## 5. 本次验证结果

已完成以下验证：

- Spring Boot 编译成功。
- Vue 前端构建成功。
- MinIO Bucket 内可以看到上传后的头像文件。
- 用户资料接口能够返回头像的签名访问地址。
- 保存个人资料后，个人资料页与顶部导航栏可以展示新头像。

## 6. 日常启动与关闭

### 开发前启动顺序

```powershell
docker start minio
```

然后确认 MySQL 已启动，再启动 Spring Boot，最后在 `web` 文件夹执行：

```powershell
npm run dev
```

MinIO 管理地址：

```text
http://localhost:9001
```

### 收工时关闭 MinIO

```powershell
docker stop minio
```

停止容器不会删除 Bucket 或已上传的图片；下次执行 `docker start minio` 即可继续使用。

## 7. 后续可完善项

1. 用户更换头像时，删除 MinIO 中旧头像，避免长期留下无用文件。
2. 增加更严格的图片内容校验，而不仅仅依赖 MIME 类型。
3. 在头像上传时加入图片裁剪与压缩。
4. 生产环境不要使用本机 `localhost`，应改为正式 MinIO 域名，并通过 HTTPS 访问。
