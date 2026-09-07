# 02 登录注册与 JWT 鉴权

## 完整链路

```text
LoginView
  → POST /api/auth/register 或 /login
  → UserController
  → UserService
  → UserRepository / BCrypt / JwtService
  → 登录响应返回 accessToken
  → httpClient 保存并在后续请求添加 Bearer Token
  → JwtAuthenticationFilter 解析身份
```

## 注册：密码为什么不会明文入库

`UserService.register` 的关键代码：

```java
if (userRepository.existsByUsername(username)) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
}

String passwordHash = passwordEncoder.encode(request.password());
UserEntity user = new UserEntity(username, passwordHash, email, LocalDateTime.now());
user.setPublicId(publicIdService.generateUniquePublicId());
user.setAvatar(DEFAULT_AVATAR);
return toResponse(userRepository.save(user));
```

- 先查用户名和邮箱是否重复，冲突返回 409。
- `PasswordEncoder.encode` 产生 BCrypt 哈希；数据库保存 `passwordHash`，不是原密码。
- BCrypt 自带随机盐，同一密码多次哈希结果通常不同。
- `toResponse` 转成 DTO，避免把 `passwordHash` 返回浏览器。
- `publicId` 用于对外展示和好友搜索；数据库内部关联仍使用自增 `id`。

## 登录：比较哈希并签发 Token

```java
UserEntity user = userRepository.findByUsername(username)
    .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.UNAUTHORIZED, "用户名或密码错误"));

boolean passwordMatches = passwordEncoder.matches(password, user.getPasswordHash());
if (!passwordMatches) throw new ResponseStatusException(
    HttpStatus.UNAUTHORIZED, "用户名或密码错误");

String token = jwtService.generateToken(user.getId(), user.getUsername());
return new LoginResponse(token, toResponse(user));
```

“用户不存在”和“密码不正确”故意使用相同提示，避免攻击者枚举有效用户名。`matches` 会从哈希中读取算法参数和盐，再验证明文。

## JWT 的内容与签名

```java
return Jwts.builder()
    .subject(username)             // sub：当前用户名
    .claim("userId", userId)       // 自定义声明
    .issuedAt(now)                 // iat：签发时间
    .expiration(expiration)        // exp：过期时间
    .signWith(secretKey)           // 使用后端密钥签名
    .compact();
```

JWT 通常是可解码的，不应在其中存密码或秘密。安全性来自签名不可伪造和后端校验，不来自“把内容加密”。当前后端业务主要使用 `sub` 中的用户名定位用户。

## JWT Filter 运行顺序

```java
String header = request.getHeader("Authorization");
if (header != null && header.startsWith("Bearer ")) {
    String token = header.substring(7);
    try {
        String username = jwtService.extractUsername(token);
        var authentication = new UsernamePasswordAuthenticationToken(
            username, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (Exception exception) {
        // 无效 Token 不建立身份，后续安全规则会拒绝受保护接口
    }
}
filterChain.doFilter(request, response);
```

- `OncePerRequestFilter` 保证一次请求只执行一次核心过滤逻辑。
- `substring(7)` 去掉 `Bearer ` 七个字符。
- `parseSignedClaims` 会校验签名和过期时间。
- Authentication 放入 `SecurityContext` 后，Controller 才能接收 `Authentication` 参数。
- 无论有没有 Token 都必须继续过滤器链；最终是否放行由 Security 配置决定。

## SecurityConfig 为什么是无状态

```java
.csrf(csrf -> csrf.disable())
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.authorizeHttpRequests(auth -> auth
    .requestMatchers(this::isPublicRequest).permitAll()
    .anyRequest().authenticated())
.addFilterBefore(jwtAuthenticationFilter,
    UsernamePasswordAuthenticationFilter.class)
```

`STATELESS` 表示服务器不依赖 HttpSession 保存登录状态，每次请求都校验 Token。CSRF 被关闭是因为这里使用 Authorization Header，而不是自动携带的 Session Cookie；若以后改为 Cookie 保存 Token，应重新设计 CSRF 防护。

## 评委可能追问

**为什么 JWT Filter 放在 UsernamePasswordAuthenticationFilter 前？** 先从 Token 恢复身份，让后续授权阶段看到已经认证的用户。

**JWT 放 localStorage 有什么风险？** 若页面发生 XSS，脚本可能读取 Token。当前方案简单，但生产系统可考虑 HttpOnly Cookie、严格 CSP 和 CSRF 策略。

**没有角色列表说明什么？** 当前完成认证和资源所有权隔离，没有完整 RBAC，不能声称实现管理员/普通用户角色系统。
