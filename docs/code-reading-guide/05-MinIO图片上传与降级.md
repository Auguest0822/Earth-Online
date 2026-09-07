# 05 MinIO 图片上传与降级

## 为什么 MySQL 不直接存图片

MySQL 保存事记及图片对象键，例如：

```text
records/alice/550e8400-e29b-41d4-a716-446655440000.png
```

图片主体存入 MinIO。这样结构化查询与大文件存储职责分离，Bucket 可以保持私有，读取时再生成短时效 URL。

## 上传接口

```java
@PostMapping(value = "/images",
             consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Map<String, String> uploadImage(
        Authentication authentication,
        @RequestParam("file") MultipartFile file) {
    String objectKey = storageService.uploadRecordImage(
        authentication.getName(), file);
    return Map.of("objectKey", objectKey);
}
```

用户名来自 Authentication，不让前端指定存储目录。接口返回对象键，随后新增事记时把对象键写入 `record_photos`。

## 文件校验

```java
private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
    "image/jpeg", "image/png", "image/webp");

if (file == null || file.isEmpty()) { /* 400 */ }
if (file.getSize() > MAX_FILE_SIZE) { /* 400 */ }
if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) { /* 400 */ }
```

校验能限制常见误传和资源消耗。答辩时可以进一步指出：仅信任客户端 MIME 仍不够严密，生产环境可读取文件魔数或用图片解码器二次验证。

## 服务端生成对象名

```java
String objectName = folder + "/" + username + "/"
        + UUID.randomUUID() + extension;
```

- UUID 防止重名覆盖。
- 用户目录方便所有权校验和运维管理。
- 不直接使用原文件名，减少路径穿越、特殊字符和重名风险。

## 流式上传

```java
minioClient.putObject(PutObjectArgs.builder()
    .bucket(bucket)
    .object(objectName)
    .stream(file.getInputStream(), file.getSize(), -1L)
    .contentType(file.getContentType())
    .build());
```

使用输入流避免把整个文件再次复制到内存。`ensureBucketExists` 在 Bucket 不存在时创建，适合本地开发；生产环境通常预先创建并配置最小权限。

## 预签名 URL

```java
String signedUrl = minioClient.getPresignedObjectUrl(
    GetPresignedObjectUrlArgs.builder()
        .method(Method.GET)
        .bucket(bucket)
        .object(objectKey)
        .expiry(1, TimeUnit.HOURS)
        .build());
```

URL 一小时后失效，所以不能长期存数据库。签名参数允许浏览器临时读取私有对象，但不会暴露 MinIO 的 access key 和 secret key。

## 降级设计

```java
public String getPreviewUrlOrNull(String objectKey) {
    try {
        return getPreviewUrl(objectKey);
    } catch (ResponseStatusException exception) {
        log.warn("MinIO preview unavailable ...");
        return null;
    }
}
```

预览地址是附加展示数据。MinIO 停止时，文字记录仍可从 MySQL 返回，只是图片为空；而“上传图片”本身依赖 MinIO，失败时应明确报错，不能假装成功。

## 评委可能追问

**为什么还要校验对象键前缀？** 防止用户把另一个用户的对象键附加到自己的记录并获取其预签名 URL。

**关闭 MinIO 是否完全无影响？** 不是。图片上传和预览受影响，文字资料、登录及不依赖图片的业务可以降级运行。

