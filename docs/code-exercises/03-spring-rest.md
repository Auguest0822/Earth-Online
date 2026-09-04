# 03 Spring Boot REST

## 填空题

### S1（★）新增事记

```java
@___
public RecordResponse create(Authentication authentication,
        @___ @___ CreateRecordRequest request) {
    return recordService.create(authentication.___(), request);
}
```

### S2（★）路径参数

```java
@GetMapping("/{id}")
public RecordResponse detail(Authentication authentication, @___ Long id) {
    return recordService.findById(authentication.getName(), id);
}
```

### S3（★）DTO 校验

为正文添加“非空、最多 1500 字”，为关键词列表添加“最多 5 个”的 Bean Validation 注解。

## 纠错题

### S4（★★）泄露 Entity

```java
@GetMapping
public List<UserEntity> users() {
    return userRepository.findAll();
}
```

指出安全和分层问题，改为只返回当前用户的安全 DTO。

### S5（★★）业务规则放错层

Controller 中直接判断未来信是否到期并修改 Entity。请重构职责，说明 Controller、Service、Repository 各自应该做什么。

## 编程题

### S6（★★）搜索接口

实现 `GET /api/records/search?query=...`，长度限制 1～50，只搜索当前用户的标题、正文和关键词，返回统一 `RecordResponse`。

### S7（★★★）统一异常

实现全局异常处理：校验失败返回 400；未认证返回 401；资源不存在返回 404；业务冲突返回 409；响应至少包含 `timestamp/status/message/path`，不得把堆栈返回给前端。

