# 06 MinIO 对象存储

## 填空题

### M1（★）上传对象

```java
minioClient.putObject(PutObjectArgs.builder()
    .bucket(___)
    .object(___)
    .stream(file.getInputStream(), file.getSize(), -1)
    .contentType(___)
    .build());
```

### M2（★）安全对象键

补全推荐格式：`records/{___}/{___}.{___}`。说明为什么不能直接使用原始文件名。

### M3（★）预签名 URL

数据库应长期保存对象键还是预签名 URL？读取图片时由哪一层生成短时效 URL？

## 纠错题

### M4（★★）无校验上传

```java
String key = file.getOriginalFilename();
storage.put(key, file.getBytes());
```

至少找出四个问题并修复：空文件、大小、内容类型、重名/路径、内存占用或所有权。

### M5（★★）故障扩大

资料查询因为头像预签名失败而整体返回 500。重构为文字资料正常返回、头像地址为空，同时保留日志和上传失败提示。

## 编程题

### M6（★★）图片上传接口

实现认证后的 `POST /api/uploads/images`：只允许常见图片且不超过 5MB；对象键必须包含当前用户名目录；返回 `objectKey`，不把 MinIO 密钥暴露给前端。

### M7（★★★）对象所有权

实现图片删除或预览前的所有权校验。攻击者即使提交另一个用户的合法对象键，也不能访问或删除该对象。

