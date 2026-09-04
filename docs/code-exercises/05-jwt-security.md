# 05 JWT 与 Spring Security

## 填空题

### A1（★）提取 Token

```java
String header = request.getHeader("Authorization");
if (header != null && header.startsWith("Bearer ")) {
    String token = header.substring(___);
}
```

### A2（★）安全上下文

Token 合法后构造 Authentication，并写入：

```java
SecurityContextHolder.___().___(authentication);
```

### A3（★）密码

注册时使用 `___Encoder.encode(rawPassword)`，登录时使用 `passwordEncoder.___(rawPassword, hash)`。

## 纠错题

### A4（★★）信任前端 userId

```java
@DeleteMapping("/{id}")
public void delete(@PathVariable Long id, @RequestParam Long userId) {
    service.delete(userId, id);
}
```

修复接口并解释为什么隐藏前端按钮不能解决此漏洞。

### A5（★★）错误的公开规则

```java
.requestMatchers("/api/**").permitAll()
.anyRequest().authenticated()
```

只允许注册、登录、健康检查和静态预检公开，其余 API 认证。写出合理配置顺序。

## 编程题

### A6（★★）JWT Filter

编写 `OncePerRequestFilter` 核心逻辑：提取 Token、解析用户名、校验有效期、加载用户、设置安全上下文、无论是否有 Token 都继续过滤器链。非法 Token 不得造成 500。

### A7（★★★）隔离测试

设计集成测试：用户甲和乙各创建一条记录；甲可以读改删自己的记录，但访问乙的记录分别得到 404 或 403；验证数据库中乙的数据仍存在。

