# AI 问答流式输出（SSE）实现方案

> 编写日期：2026-09-18
> 目标功能：年度记录页 →「问问这一年」的 AI 回答改为**逐字流式输出**
> 现状：`POST /api/ai/yearly-memory/query` 一次性返回完整回答，前端 `askMemory()` 等待 1.3～4 秒后整段显示
> 相关文档：[`AI年度回忆问答缓存实现笔记.md`](./AI年度回忆问答缓存实现笔记.md)、[`AI年度回忆与RAG从零到答辩学习指南.md`](./AI年度回忆与RAG从零到答辩学习指南.md)

---

## 0. 先回答"几小时内能不能做完"

**能，但要限定范围。**

| 问题 | 结论 |
| --- | --- |
| 净工时 | **约 4.5～6 小时**（后端 2.5h + 前端 1.5h + Nginx/部署 0.5h + 联调验证 1h） |
| 适合让 AI 代做的部分 | 流式客户端、SSE 解析器、Nginx 片段、测试脚本——都是模式化代码 |
| 必须人工确认的部分 | 事务边界、鉴权异步派发、缓存落库时机（这三处出错不会编译报错，只会线上静默出错） |
| 只做「问问这一年」 | ✅ 推荐。它输出的是**纯文本**，天然适合流式 |
| 同时做「年度总结」 | ❌ 不建议。它输出的是**结构化 JSON**（title/summary/highlights/letter），流式要么改提示词格式、要么写增量 JSON 解析，工作量翻倍且容易出错 |

一句话结论：

> **真流式（SSE）4.5～6 小时可交付；如果只剩 1～2 小时，改用"前端打字机"假流式（见第 9 节），零后端风险。**

---

## 1. 先说清楚流式到底带来什么（别把预期定错）

这是答辩时最容易被追问的点：

| 指标 | 现在（非流式） | 改流式后（实测） |
| --- | --- | --- |
| 首字可见时间 | 1.3～4.0 秒（整段一起出现） | **线上 0.83 秒；本地开发机 1.2～2.1 秒** |
| 全部生成完的总耗时 | 1.3～4.0 秒 | **线上 1.47 秒；本地 2.8～3.8 秒**（一样，只是能看到过程） |
| 用户主观感受 | "卡住了，是不是坏了" | "在打字，在思考" |
| Token 费用 | 不变 | 不变 |

> 实测数据来自 2026-09-18 的验证（见第 12 节）：服务器到 DeepSeek 的网络比家用宽带好，
> 所以线上首字只要 0.8 秒左右；本地开发机因为要绕一圈网络，首字 1.2～2.1 秒。

> **流式不会让 AI 更快，它让等待"可见"。**
> 答辩时照实说："我们把**首字延迟**从 2 秒降到 0.6 秒左右，总耗时不变，但用户不再觉得卡死。"——比说"我们优化了性能"诚实得多，也更专业。

---

## 2. 方案选型：SSE / WebSocket / 轮询

| 方案 | 是否适合 | 理由 |
| --- | --- | --- |
| **SSE（`text/event-stream`）** | ✅ 选它 | 单向"服务端→浏览器"推送，正好匹配"逐字吐答案"；走标准 HTTP，复用现有 JWT 鉴权与 Nginx 反代；后端只需 `SseEmitter`，不加任何依赖 |
| WebSocket（复用现有 STOMP） | ⚠️ 可用但更重 | 项目已有 `/ws/chat`。但为一次问答维护订阅/取消订阅、消息路由、连接状态，成本比 SSE 高；WebSocket 的价值在**双向**（聊天），而这里是单向 |
| 前端轮询 | ❌ | 后端没有可轮询的"进度"，要额外做状态存储，延迟也差 |

**答辩话术**：

> "聊天用 WebSocket 是因为它是双向的；AI 回答是单向推送，用 SSE 更轻——一个普通 HTTP 请求，不需要维护连接状态，也不需要新增端口和协议，还天然复用了我们已有的 JWT 鉴权和 Nginx 转发。"

---

## 3. 后端设计

### 3.1 接口契约

新增一个接口，**保留原来的非流式接口**（作为降级与回滚路径）：

```http
POST /api/ai/yearly-memory/stream
Authorization: Bearer <token>
Content-Type: application/json

{ "year": 2025, "question": "这一年有哪些让我开心的事情？" }

→ 200 OK
Content-Type: text/event-stream;charset=UTF-8
Cache-Control: no-cache
X-Accel-Buffering: no
```

SSE 事件协议（**四种事件，前端按名字分发**）：

| 事件名 | 时机 | data 示例 | 前端动作 |
| --- | --- | --- | --- |
| `meta` | 流开始前（检索完成） | `{"year":2025,"question":"...","sources":[{...}]}` | 立刻渲染"回答依据"卡片，正文先显示光标 |
| `delta` | 每个增量片段 | `{"text":"根据你的记录，"}` | 追加到正文（逐字/逐块显示） |
| `done` | 正常结束 | `{"updatedAt":"2026-09-18T11:20:31","length":182}` | 收尾：去掉光标、显示"上次提问于…" |
| `error` | 任何失败 | `{"message":"AI 服务繁忙，请稍后重试"}` | 弹提示 + 可选自动回退到非流式接口 |

> ⚠️ **错误只能走 `error` 事件**。HTTP 状态码在第一个字节发出时就已经定型（200），此后无法再改成 500。所以：**首个 token 之前的失败**（参数错、无记录、Key 未配置）可以正常返回 JSON 错误；**已经开始输出之后的失败**必须用 `error` 事件。

### 3.2 调用链（在原结构上扩展，不动 RAG 逻辑）

```text
YearlyMemoryController.stream()
  └─ YearlyMemoryService.stream(username, year, question, emitter)
       ├─ [短事务 1] 校验用户 + 取该年记录 + 检索打分（复用现有 relevance()）→ sources
       ├─ emitter.send(meta, sources)                    ← 前端立刻看到依据
       ├─ [无事务] aiStreamClient.streamAnswer(...)       ← 逐块回调，emitter.send(delta)
       ├─ [短事务 2] 累积完整答案后落库 yearly_memory_answers（复用现有缓存逻辑）
       └─ emitter.send(done, {updatedAt})
```

**关键：检索与落库分成两段短事务，流式期间不持有数据库事务。**
一次回答要 2～4 秒，如果把 `@Transactional` 套在整个流式过程外，就会有一个数据库连接被占住 4 秒——用户一多就是连接池打满。这正是项目之前"删除接口 500"排查里学到的教训：**事务边界要贴着数据库操作，而不是贴着业务流程**。

> ⚠️ 实现细节：上面骨架里的 `self.retrieve(...)` / `self.saveAnswer(...)` 是**同一个 Service 注入自己**（`@Lazy private final YearlyMemoryService self`），或者把这两个方法挪到一个单独的 `YearlyMemoryStore` Bean 里。
> 原因是 Spring 的 `@Transactional` 靠代理生效，**同类内部直接调用 `this.retrieve()` 会绕过代理**，事务不会开启——这个坑不报错、只在并发时暴露（多连接/脏读），非常难查。

### 3.3 Controller 骨架

```java
@PostMapping("/stream")
public SseEmitter stream(Authentication authentication,
                         @Valid @RequestBody YearlyMemoryQueryRequest request,
                         HttpServletResponse response) {
    // 关掉 Nginx 缓冲（双保险，Nginx 侧也会配 proxy_buffering off）
    response.setHeader("X-Accel-Buffering", "no");
    // 超时必须显式设置：不设的话取容器默认（约 30 秒），长回答会被掐断
    SseEmitter emitter = new SseEmitter(180_000L);
    // 用专门的线程池执行，不要占用 Tomcat 请求线程
    aiStreamExecutor.execute(() -> service.stream(authentication.getName(),
            request.year(), request.question(), emitter));
    return emitter;
}
```

要点：

1. `SseEmitter` 来自 `spring-webmvc`（项目是 **webmvc 不是 webflux**，所以**不要**返回 `Flux`，那需要引入 WebFlux 并切换整个 Web 栈，绝对不要做）；
2. 超时必须在构造时给（180 秒 > DeepSeek 读超时 60 秒）；
3. 后台任务必须用**自己的线程池**：Tomcat 的请求线程在 `startAsync` 后会被释放，但 `ForkJoinPool.commonPool()` 在 2 核服务器上会和其它任务抢线程。

```java
// config/AiStreamConfig.java（新增）
@Bean("aiStreamExecutor")
public ThreadPoolTaskExecutor aiStreamExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(20);
    executor.setThreadNamePrefix("ai-stream-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

### 3.4 流式客户端骨架（新增 `YearlyMemoryStreamClient`）

DeepSeek 的流式响应是标准 OpenAI 格式的 SSE，用 JDK `HttpClient` 直接读行流即可（**不要用 `RestClient`**，它是阻塞取完整响应的）：

```java
public void streamAnswer(int year, String question, List<RecordEntity> records,
                         Consumer<String> onDelta) {
    Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", buildPrompt(year, question, records))),
            "temperature", 0.3,
            "max_tokens", 700,
            "stream", true);                    // ← 与原客户端的唯一区别

    HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/chat/completions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .timeout(Duration.ofSeconds(20))    // 只约束"等到响应头"，不约束流式读取
            .POST(HttpRequest.BodyPublishers.ofString(toJson(body)))
            .build();

    HttpResponse<Stream<String>> response =
            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
    if (response.statusCode() != 200) {
        throw mapStatus(response.statusCode());     // 429 / 402 / 401 → 对应中文提示
    }
    try (Stream<String> lines = response.body()) {
        lines.forEach(line -> {                     // 逐行读，收到一行就推一行
            if (line.isBlank() || line.startsWith(":")) return;          // 心跳/空行
            if (!line.startsWith("data:")) return;                        // event:/id: 等
            String payload = line.substring(5).trim();
            if ("[DONE]".equals(payload)) return;
            String delta = readDelta(payload);                            // Jackson 2 解析 choices[0].delta.content
            if (!delta.isEmpty()) onDelta.accept(delta);
        });
    }
}
```

解析增量的注意点（都是真实会遇到的）：

| 现象 | 处理 |
| --- | --- |
| `delta.content` 为 `null`（例如最后一个 chunk 只带 `finish_reason`） | `asText("")`，空串跳过 |
| 收到 `data: [DONE]` | 正常结束标记，不是内容 |
| 收到 `: keep-alive` 注释行 | 跳过 |
| JSON 解析失败 | 记 warn 日志并跳过该 chunk，不要中断整个流 |
| 客户端已断线 | `emitter.send()` 抛 `IOException` → 立刻跳出循环并关闭上游流（省 token） |

> 仍然遵守那条铁律：**这套代码用的是 Jackson 2，任何 DTO 里不要出现 `java.time` 类型**（上次 `LocalDate` 导致的 500 就是这么来的）。流式解析里只有字符串，安全。

### 3.5 Service 骨架（含缓存联动与降级）

```java
public void stream(String username, int year, String question, SseEmitter emitter) {
    StringBuilder answer = new StringBuilder();
    try {
        // ── 短事务 1：检索（复用现有 query() 的前半段，抽成 retrieve()） ──
        RetrievalResult retrieval = self.retrieve(username, year, question);
        send(emitter, "meta", metaJson(retrieval));      // sources 先给前端

        // ── 无事务：流式生成 ──
        streamClient.streamAnswer(year, question, retrieval.records(), delta -> {
            answer.append(delta);
            send(emitter, "delta", deltaJson(delta));
        });

        // ── 短事务 2：落库（复用现有缓存写入，失败也不影响已经给用户的答案） ──
        LocalDateTime updatedAt = self.saveAnswer(username, year, question, answer.toString(), retrieval);
        send(emitter, "done", doneJson(updatedAt, answer.length()));
        emitter.complete();
    } catch (ResponseStatusException e) {
        sendError(emitter, e.getReason());               // 只能走 error 事件
        emitter.complete();
    } catch (Exception e) {
        log.error("流式回答失败", e);
        sendError(emitter, "AI 服务暂时不可用，请稍后重试");
        emitter.complete();
    }
}
```

三个必须处理的边界：

| 边界 | 设计 |
| --- | --- |
| 落库失败（比如 `sources_json` 序列化异常） | **不能影响已经流给用户的答案**：catch 住记日志，`done` 事件照发但 `updatedAt` 为空（前端不显示"上次提问于"） |
| 用户在流中途刷新/关闭页面 | 客户端断开 → `send()` 抛异常 → 停止上游调用；**不落库**（答案不完整，避免把半截回答当成缓存） |
| 同一用户并发提问 | 每次都是独立的 emitter，互不影响；落库是"后写覆盖"，与现有覆盖式缓存语义一致 |

### 3.6 鉴权：一个必须验证的检查点

问题背景：项目是 `OncePerRequestFilter` + Spring Security 6/7，而 SSE 会触发**容器异步派发（ASYNC dispatch）**。默认情况下 Spring Security 会对 ASYNC 派发**再次执行鉴权**，此时如果 `SecurityContext` 没有被恢复到该派发上，异步派发就会被判定成未登录，出现下面两种诡异现象之一：

- 流正常输出，但在**结束那一刻**浏览器控制台报 401；
- 响应体末尾被追加上一段 `{"status":401,...}` 的 JSON（SecurityConfig 的 entry point 写的），SSE 解析直接报错。

**实现时的验证方法**（1 分钟能测出来）：

```bash
curl -N -X POST http://127.0.0.1:8080/api/ai/yearly-memory/stream \
  -H "Authorization: Bearer <本地登录拿到的 token>" \
  -H "Content-Type: application/json" \
  -d '{"year":2025,"question":"这一年有哪些开心的事情？"}' | tail -5
```

末尾必须是干净的 `event: done` + `data: {...}`，**不能**出现 401 文案。

**修复方式**（只加一行，且不影响正常请求鉴权）——**本次实现时确实复现了这个故障，已按下述方式修复**：

```java
.authorizeHttpRequests(auth -> auth
        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()   // ← SSE 异步派发放行
        .requestMatchers(this::isPublicRequest).permitAll()
        .anyRequest().authenticated()
)
```

当时的真实报错（`docker logs` / 本地控制台里就是这个）：

```text
AuthorizationDeniedException: Access Denied
    at AuthorizationFilter.doFilter(AuthorizationFilter.java:99)
    ...
    at CoyoteAdapter.asyncDispatch(CoyoteAdapter.java:238)
Unable to handle the Spring Security Exception because the response is already committed.
```

现象是"回答能流出来，但在结束那一刻连接被硬生生掐断"，客户端报 `TypeError: terminated`。
所以这一节不是"可能遇到的问题"，而是**一定会遇到的问题**——照抄这一行即可。

安全性说明（答辩可能被问）：ASYNC 派发**不是新的外部请求**，它是同一个请求的继续，第二次派发时既没有新的用户输入，也没有新的 URL 参数；真正的入口请求（`DispatcherType.REQUEST`）仍然完整地走了 JWT 鉴权，所以放行 ASYNC 不会造成越权。

### 3.7 前端为什么不能用 `EventSource`

| 限制 | 影响 |
| --- | --- |
| `EventSource` 只能发 GET，不能带请求体 | 我们的 year + question 是 JSON body，塞到 query string 里既要转义、又会把问题内容写进 Nginx 访问日志 |
| `EventSource` 不能自定义请求头 | 带不了 `Authorization: Bearer <token>`（项目 token 在 localStorage，不是 Cookie） |
| 用 query 参数传 token | **绝对不要**：token 会进 access log、浏览器历史、Referer |

**所以用 `fetch` + `ReadableStream` 手动解析 SSE**（浏览器完全支持，代码量约 40 行）。

---

## 4. 前端设计

### 4.1 新增流式请求模块 `web/src/api/modules/aiStream.js`

```js
import { API_BASE_URL } from '../config'
import { ApiError, clearAccessToken, getAccessToken } from '../httpClient'

// SSE 帧解析：把 "event: delta\ndata: {...}" 变成 { event, data }
const parseFrame = raw => {
  let event = 'message'
  const dataLines = []
  raw.split(/\r?\n/).forEach(line => {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
  })
  return { event, data: dataLines.join('\n') }
}

export const streamYearlyMemory = async (year, question, handlers, signal) => {
  const response = await fetch(`${API_BASE_URL}/ai/yearly-memory/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${getAccessToken()}`
    },
    body: JSON.stringify({ year, question }),
    signal
  })

  // 首个 token 之前的错误仍是普通 JSON，按老逻辑处理
  if (!response.ok) {
    const contentType = response.headers.get('content-type') || ''
    const payload = contentType.includes('application/json') ? await response.json() : null
    if (response.status === 401) {            // 流式接口绕过了 httpClient，要自己处理登录失效
      clearAccessToken()
      window.location.replace(`/login?expired=1&redirect=${encodeURIComponent(window.location.pathname)}`)
    }
    throw new ApiError(payload?.message || `请求失败（${response.status}）`, response.status, payload)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    let index
    while ((index = buffer.search(/\r?\n\r?\n/)) >= 0) {
      const frame = buffer.slice(0, index)
      buffer = buffer.slice(index + (buffer[index] === '\r' ? 4 : 2))
      if (!frame.trim()) continue
      const { event, data } = parseFrame(frame)
      if (!data) continue
      handlers[event]?.(JSON.parse(data))
    }
  }
}
```

细节提醒：

- **15 秒防抖**：`handlers.delta` 里不要每个 token 都写 `ref`，按时间片（约 40ms）合并后再赋值，减少 Vue 重渲染；
- **组件卸载要 abort**：`onBeforeUnmount(() => controller.abort())`，否则用户切页面后流还在跑；
- `done` 事件后 `reader` 自然结束，不需要手动关闭。

### 4.2 `YearlyView.vue` 改造点

改动很小，因为**模板几乎不用改**——`.memory-answer` 里那套（回答正文、`.answer-time`、过期提醒、依据卡片）都能直接复用：

```js
const askMemory = async () => {
  if (!canAskMemory.value) return
  memoryLoading.value = true
  memoryAnswer.value = null
  streamedText.value = ''
  controller = new AbortController()
  try {
    await streamYearlyMemory(Number(selectedYear.value), memoryQuestion.value.trim(), {
      meta: payload => { memoryAnswer.value = { ...payload, answer: '', stale: false } },
      delta: payload => { pendingText += payload.text; scheduleFlush() },
      done: payload => { memoryAnswer.value = { ...memoryAnswer.value, answer: streamedText.value, updatedAt: payload.updatedAt } },
      error: payload => { ElMessage.error(payload.message || 'AI 回忆问答失败') }
    }, controller.signal)
  } catch (error) {
    if (error.name !== 'AbortError') {
      // 流式失败 → 自动回退到原来的非流式接口，保证功能可用
      memoryAnswer.value = await queryYearlyMemory(Number(selectedYear.value), memoryQuestion.value.trim())
    }
  } finally {
    memoryLoading.value = false
  }
}
```

模板只加一个"生成中"光标：

```html
<p>{{ memoryAnswer.answer }}<span v-if="memoryLoading" class="stream-cursor"></span></p>
```

```scss
.stream-cursor {
  display: inline-block;
  width: 2px; height: 1em;
  margin-left: 2px;
  background: $primary-color;
  animation: blink 1s steps(2, start) infinite;
  vertical-align: text-bottom;
}
@keyframes blink { to { visibility: hidden; } }
```

---

## 5. Nginx 与部署

SSE 最常见的线上故障是"**本地正常、线上一次性吐出全部内容**"——原因是 Nginx 默认开启 `proxy_buffering`，会把响应攒起来再发。**不要动全局 `/api/` 配置**，只给流式路径开一条精确匹配：

```nginx
# 流式接口：必须关闭缓冲，否则前端要等整段生成完才显示（等于白做）
location = /api/ai/yearly-memory/stream {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_buffering off;          # 关键
    proxy_cache off;              # 关键
    gzip off;                     # 压缩也会攒缓冲
    proxy_read_timeout 180s;      # 与 SseEmitter 超时对齐
    chunked_transfer_encoding on;
}
```

部署步骤（沿用现有流程）：

```bash
# 本地
scp -i $key "E:\Project\Earth-Online\deploy\nginx-earth-online.conf" "root@$serverIp:/tmp/earth-online-nginx.conf"
scp -i $key "E:\Project\Earth-Online\backend\target\backend-0.0.1-SNAPSHOT.jar" "root@$serverIp:/tmp/backend-new.jar"
scp -i $key -r "E:\Project\Earth-Online\web\dist" "root@$serverIp:/tmp/earth-online-dist"

# 服务器
cp /etc/nginx/sites-available/earth-online /root/earth-online-nginx-$(date +%F-%H%M).conf.bak
cp /tmp/earth-online-nginx.conf /etc/nginx/sites-available/earth-online
nginx -t && systemctl reload nginx
cd /opt/earth-online/deploy && cp /tmp/backend-new.jar ../backend/target/backend-0.0.1-SNAPSHOT.jar
docker compose --env-file ../.env up -d --build --force-recreate backend
rm -rf /var/www/earth-online/* && cp -r /tmp/earth-online-dist/* /var/www/earth-online/
chown -R www-data:www-data /var/www/earth-online && systemctl reload nginx
```

**本次不需要改 `.env`、不需要改 MySQL、不需要新依赖**（`SseEmitter` 在已有的 `spring-boot-starter-webmvc` 里）。

---

## 6. 时间预算（净工时）

| 阶段 | 内容 | 耗时 |
| --- | --- | --- |
| 后端 1 | `YearlyMemoryStreamClient`（JDK HttpClient 逐行解析 + 错误映射） | 1.0 h |
| 后端 2 | Service 拆 `retrieve()` / `saveAnswer()`，Controller + 线程池 | 1.0 h |
| 后端 3 | 鉴权 ASYNC 检查点验证 + 边界（断线、落库失败、无 Key） | 0.5 h |
| 前端 1 | `aiStream.js`（fetch 流解析、401 处理、abort） | 1.0 h |
| 前端 2 | `YearlyView.vue` 增量渲染 + 光标 + 回退 + 卸载 abort | 0.5 h |
| 部署 | Nginx 精确 location + 上传 + reload | 0.5 h |
| 验证 | curl 时序、DevTools EventStream、断线、缓存落库、降级、回归 | 1.0 h |
| **合计** | | **≈ 5.5 h** |

可压缩点：让 AI 一次性产出 1、2、4、5 的代码骨架（模式化），人只做第 3 步的边界确认和验证，**实际约 4 小时**。

### 建议的实施顺序（每步都有可验证的产出，别一次写完再测）

| 步骤 | 做什么 | 做完就验证什么 |
| --- | --- | --- |
| 1 | 后端：`YearlyMemoryStreamClient` + 一个临时的 `main`/测试方法 | 控制台能逐行打印 DeepSeek 的增量文本 |
| 2 | 后端：Controller + Service 的 `stream()`，先不做落库 | `curl -N` 能看到 `meta`/`delta`/`done` 三种事件，且**末尾没有 401**（第 3.6 节检查点） |
| 3 | 后端：补两段短事务与落库 | 流结束后 `GET /answer` 能读到完整缓存 |
| 4 | 前端：`aiStream.js` + `YearlyView.vue` 增量渲染 | 本地页面逐字显示，切路由请求被 cancel |
| 5 | 部署：Nginx 精确 location | 走公网 `curl -N`，确认**不是一次性全出** |
| 6 | 回归 + 边界 | 第 7 节清单 10 项逐条过 |

---

## 7. 验证清单（做完必须逐条过）

| # | 验证项 | 方法 | 期望 |
| --- | --- | --- | --- |
| 1 | 首字延迟 | `curl -N` + 时间戳脚本（或浏览器 Network → EventStream 时间轴） | 首字 < 1.2s，明显早于结束 |
| 2 | 事件协议 | `curl -N` 看原始输出 | 先 `meta`（含 sources），再大量 `delta`，最后 `done` |
| 3 | 无 401 尾巴 | `curl -N` 后看输出的最后 5 行 | 结尾只有 `done`，没有 JSON 错误体 |
| 4 | 线上不缓冲 | 部署后重复第 1 步（走 Nginx） | 与本地一致，**不是**一次性全出 |
| 5 | 缓存落库 | 流结束后 `GET /api/ai/yearly-memory/answer?year=2025` | 返回完整答案，`stale=false`，`updatedAt` 是刚刚 |
| 6 | 中途断开不落库 | 流到一半刷新页面，再查缓存 | 仍是上一次的完整问答（半截答案没有被写入） |
| 7 | 降级 | 故意把 `DEEPSEEK_API_KEY` 配错重启 | 前端弹提示；非流式接口同样报错 → 说明是 Key 问题不是流式问题 |
| 8 | 前端回退 | DevTools 里把 `/stream` 请求改成失败（或临时改错路径） | 前端自动回退到 `POST /query`，功能仍可用 |
| 9 | 页面卸载 | 生成中切到别的路由 | Network 里该请求被 cancel，后端日志显示上游已中断 |
| 10 | 回归 | 年度总结、年度记录页图表、非流式问答 | 全部不受影响 |

---

## 8. 风险清单与回滚

| 风险 | 概率 | 影响 | 对策 |
| --- | --- | --- | --- |
| Spring Security ASYNC 派发导致 401/响应被污染 | 中 | 流末尾报错 | 第 3.6 节的检查点 + 一行 `DispatcherType.ASYNC` 放行 |
| Nginx 缓冲导致"假流式" | 中 | 体验同改前 | 精确 location + `proxy_buffering off`，部署后必测第 4 项 |
| 流式期间占用数据库连接 | 低 | 并发下连接池耗尽 | 两段短事务，流式过程无事务 |
| 落库失败导致用户看到答案但没缓存 | 低 | 下次进页面没有缓存 | try/catch 隔离，`done` 事件不带 `updatedAt` |
| 客户端断线时上游仍在生成 | 中 | 白花 token | `send()` 异常 → 立即跳出并关闭上游流 |
| 代理/浏览器把长连接掐断 | 低 | 回答中断 | `SseEmitter(180s)` + `proxy_read_timeout 180s` 对齐 |

**回滚**：删掉前端调用流式接口的那一行、改回 `queryYearlyMemory`，后端新接口留着不用即可——**非流式接口全程保留**，这就是最简单的回滚开关（也可以加 `ai.stream.enabled` 配置项做成开关）。

---

## 9. 备选：1～2 小时的"假流式"（打字机效果）

如果时间不够，或者答辩前不想动后端：

```js
// 一次性拿到完整答案，然后前端逐字显示
const full = (await queryYearlyMemory(year, question)).answer
for (let i = 0; i < full.length; i += 2) {
  memoryAnswer.value.answer = full.slice(0, i + 2)
  await new Promise(r => setTimeout(r, 25))
}
```

| 对比 | 假流式 | 真流式（本文方案） |
| --- | --- | --- |
| 工时 | 0.5～1 h | 4～6 h |
| 首字可见 | 约等于 AI 返回时间（**没有改善**） | 0.4～1.0 s |
| 后端风险 | 零 | 中（异步派发、缓冲） |
| 答辩含金量 | 低（"前端动画"） | 高（"SSE 服务端推送 + 上游流式转发"） |

**建议**：时间够就做真流式；只是为了演示观感，假流式也能救场，但答辩时**必须诚实说明**是前端动画，不要包装成 SSE。

---

## 10. 答辩问答准备

**Q：为什么 AI 回答用 SSE，聊天却用 WebSocket？**
> 聊天是双向的（两边都要主动发消息），WebSocket 合适；AI 回答是服务端单向推送，SSE 走普通 HTTP 就够，不用维护连接状态、不用新协议，还能直接复用现有 JWT 鉴权和 Nginx 配置。

**Q：流式之后回答变快了吗？**
> 总耗时没变，变的是首字延迟——线上从原来的 2 秒左右（等整段生成完）降到 0.8 秒左右，用户能立刻看到内容在生成，不会以为页面卡死。这是感知性能的优化，不是吞吐优化。

**Q：流式过程中如果用户关掉页面怎么办？**
> 客户端断开会触发 `SseEmitter` 写入异常，后端立刻停止向 DeepSeek 读取并关闭上游连接，避免继续消耗 token；同时这次不完整的回答**不会**写入缓存，用户下次进来看到的仍是上一次完整的问答。

**Q：为什么年度总结不一起做流式？**
> 年度总结的输出是结构化 JSON（标题、亮点、关键词、给未来的信），流式输出需要改造成增量 JSON 解析或者分行协议，收益和风险不成正比；总结本身有缓存，第二次进入是 0 延迟的，所以我们对它做的是缓存优化，对问答做的是流式优化——**按使用频率和收益选择优化手段**。

**Q：为什么不用 LangChain / Spring AI 的流式能力？**
> 当前链路只有「检索 → 调模型 → 落库」三步，直接用 `SseEmitter` + JDK `HttpClient` 大约 60 行代码就能完成，不引入框架意味着调试和排查更直接；等出现多轮工具调用、分支状态时再考虑框架更划算。

**Q：为什么有时候回答是"一下子全出现"的？**
> 那是**缓存命中**：进入页面时会读取上一次的问答（存在 MySQL 里），大约 60 毫秒就返回，所以看起来是瞬间出现——这正是缓存要的效果。点「重新提问」才会走 SSE 让 AI 实时生成，此时正文是逐字写进去的。页面上用一个标签区分这两种来源（`来自上次问答` / `实时生成中…`），演示时先说明这一点，避免评委误以为流式没生效。

---

## 11. 改动文件清单（实现时按此提交）

| 文件 | 类型 | 说明 |
| --- | --- | --- |
| `backend/.../ai/YearlyMemoryStreamClient.java` | 新增 | JDK HttpClient 逐行解析 DeepSeek 流式响应 |
| `backend/.../ai/YearlyMemoryPrompt.java` | 新增 | 流式/非流式共用的提示词（避免两边写法跑偏） |
| `backend/.../ai/dto/YearlyMemoryStreamEvents.java` | 新增 | `meta` / `delta` / `done` / `error` 四种事件负载 |
| `backend/.../ai/YearlyMemoryStore.java` | 新增 | 检索与落库两段短事务（从 Service 拆出，避免自调用绕过事务代理） |
| `backend/.../config/AiStreamConfig.java` | 新增 | `aiStreamExecutor` 线程池 |
| `backend/.../ai/YearlyMemoryService.java` | 修改 | 变成编排层：`getSaved` / `query` / `prepare` / `stream` |
| `backend/.../ai/YearlyMemoryController.java` | 修改 | 新增 `POST /stream`，返回 `SseEmitter` |
| `backend/.../ai/YearlyMemoryAiClient.java` | 修改 | 改用共享提示词，删掉重复代码 |
| `backend/.../config/SecurityConfig.java` | 修改 | 放行 `DispatcherType.ASYNC`（实测必须，见 3.6） |
| `web/src/api/modules/aiStream.js` | 新增 | fetch 流式请求 + SSE 帧解析 |
| `web/src/views/YearlyView.vue` | 修改 | 增量渲染、光标、abort、回退到非流式 |
| `deploy/nginx-earth-online.conf` | 修改 | 流式路径精确 location + `proxy_buffering off` |
| `docs/AI问答流式输出SSE实现方案.md` | 本文 | 方案与验证记录 |

**不需要改**：数据库表结构、`application.yaml`、`.env`、`docker-compose.yml`、依赖（`pom.xml`）。

---

## 12. 实现与验证记录（2026-09-18）

### 12.1 与方案不同的两处（实现时发现的）

| 计划 | 实际做法 | 原因 |
| --- | --- | --- |
| 检索也在异步线程里做 | 检索改为在**请求线程**同步做（`service.prepare()`），异步线程只负责流式生成 | 检索失败（例如该年没有记录）时响应还没提交，异常可以正常走全局异常处理器返回 **400 JSON**；如果放到异步线程里，HTTP 状态码已经定型成 200，只能退回 `error` 事件——前端就拿不到标准的错误码了 |
| 没料到必须放行 `DispatcherType.ASYNC` | 确实必须，已改 `SecurityConfig` | 本地第一次跑就复现了 3.6 节的故障：`AuthorizationDeniedException` + `Unable to get the Spring Security Exception because the response is already committed`，客户端报 `TypeError: terminated` |

### 12.2 实测数据

| 指标 | 本地开发机 | 线上服务器 |
| --- | --- | --- |
| 首字延迟 | 1.2～2.1 秒 | **0.83 秒** |
| 生成总耗时 | 2.8～3.8 秒 | **1.47 秒** |
| 增量块数量 | 110～115 个 delta | 119 个 delta |
| 分块到达 | —— | 52 次，跨 1271ms（首个分块 185ms） |

分块到达时间（线上，前 12 次）：

```text
185, 832, 882, 898, 899, 909, 923, 938, 952, 964, 978, 992 (ms)
```

这张时间表就是"**Nginx 没有缓冲**"的证据：如果是缓冲模式，所有分块会在最后同一时刻一起到达。

### 12.3 验证结果

| 阶段 | 内容 | 结果 |
| --- | --- | --- |
| 本地接口 | 34 项断言：事件协议（meta→delta→done）、增量块数量、`done.length` 与拼接结果一致、无 401 尾巴、缓存落库、**断线不落库**、无记录年份返回 400 JSON、非流式接口回归、未登录 401 | **34/34 通过** |
| 本地界面 | 真实 Chrome：采样正文长度从 0 → 30 → 178 逐步增长、生成中显示光标、结束时显示"上次提问于"、页面内容与缓存一致 | **12/12 通过** |
| 线上接口 | 21 项断言：含分块到达时序（证明未被 Nginx 缓冲）、断线不落库、缓存一致 | **21/21 通过** |
| 线上界面 | 真实 Chrome 打公网地址：进入页面读缓存 189 字、提问后约 0.6 秒出现首字、1.05 秒完成、与缓存一致 | **9/9 通过** |

回归验证：非流式接口 `POST /query`、缓存读取 `GET /answer`、年度总结均未受影响。

### 12.4 上线步骤（已执行）

```text
备份：~/earth_online_before_sse.sql（51K）、旧 jar、旧 dist、旧 nginx 配置
上传：jar → /tmp/backend-new.jar；dist → /tmp/earth-online-dist；nginx → /tmp/earth-online-nginx.conf
服务器：
  cp /tmp/backend-new.jar backend/target/backend-0.0.1-SNAPSHOT.jar
  docker compose --env-file ../.env up -d --build --force-recreate backend
  cp nginx 配置 → nginx -t → systemctl reload nginx
  替换 /var/www/earth-online + chown www-data
验证：健康检查 200、流式接口未登录 401、前端 200、公网 curl -N 分块时序正常
```

**踩到的一个坑**：第一次部署后前端资源名没变（`YearlyView-BaKAwurn.js`），说明上传的是**旧 dist**——
因为我改完前端只跑了 `npm run dev`（开发服务器实时编译），忘了重新 `npm run build`。
重新构建后资源名变成 `YearlyView-B2ObuRID.js`，问题解决。
**教训：每次部署前都要用"资源哈希是否变化"来确认 dist 真的是新构建的产物**（和之前"忘了 build 就上传"是同一类错误）。

### 12.5 回滚方式

```bash
# 后端
cp ~/earth-online-backup/<时间>-backend-before-sse.jar /opt/earth-online/backend/target/backend-0.0.1-SNAPSHOT.jar
cd /opt/earth-online/deploy && docker compose --env-file ../.env up -d --build --force-recreate backend
# 前端
tar -xzf ~/earth-online-backup/<时间>-web-before-sse.tar.gz -C /var/www && chown -R www-data:www-data /var/www/earth-online
# Nginx
cp ~/earth-online-backup/<时间>-nginx-before-sse.conf /etc/nginx/sites-available/earth-online && nginx -t && systemctl reload nginx
```

也可以只改前端一处：把 `YearlyView.vue` 的 `askMemory()` 换回 `queryYearlyMemory(...)`，
后端流式接口留着不用即可（**非流式接口从头到尾都在，这就是最省事的降级开关**）。

---

## 13. 上线后的一次"看起来没生效"反馈与排查（2026-09-18）

### 13.1 反馈

上线后自测反馈："页面上返回的 AI 内容是**一口气生成**的，不是逐字流出，SSE 没有达到效果。"

### 13.2 排查：不看猜测，先看访问日志

```bash
grep "yearly-memory" /var/log/nginx/access.log | tail -40
```

关键几行（已省略 UA）：

```text
15:50:11  POST /api/ai/yearly-memory/query   200 1738    ← 用的是老前端包（非流式接口）
15:55     （部署新版本，Nginx reload）
16:05:12  GET  /api/ai/yearly-memory/answer?year=2026  200 1747   ← 只读缓存
16:05:42  GET  /api/ai/yearly-memory/answer?year=2026  204 0      ← 只读缓存（另一个账号）
16:06:06  GET  /api/ai/yearly-memory/answer?year=2026  204 0
```

结论一目了然：

1. **15:50 那次提问发生在部署之前**（部署时间是 15:55），当时线上还是旧前端，调用的正是非流式接口 `POST /query` —— 那次"一口气出现"是**正常的旧行为**，不是流式失效。
2. 部署之后，浏览器发出的请求**只有 `GET /answer`（读缓存），没有任何 `POST /stream`**。
   也就是说：**用户看到的是"缓存恢复"，不是"AI 生成"。** 缓存是直接从 MySQL 读出来的，前端实测 **35～62 毫秒**就把整段回答渲染出来——肉眼当然就是"一口气出现"。
3. 与此同时，部署后的包里流式路径是好的：headless Chrome 从公网地址实测，`POST /stream 200`，正文在浏览器里被改写 14 次、出现 10 个不同长度：

```text
2 → 20 → 34 → 50 → 77 → 99 → 123 → 140 → 162 → 172 （字符数，总耗时 1176ms）
```

### 13.3 根因：两种模式在界面上长得一模一样

| 场景 | 实际行为 | 界面表现（修改前） |
| --- | --- | --- |
| 进入页面 / 切换年份 | 读缓存（约 50ms） | 回答"瞬间出现" |
| 点「开始回忆」 | SSE 流式生成 | 回答逐字出现 |

两个场景的标题、正文、按钮长得完全一样，用户很自然会把"缓存恢复"当成"流式没生效"。
**这不是流式的 bug，是交互表达的问题。**

### 13.4 修复：把两种来源显式标出来

| 改动 | 效果 |
| --- | --- |
| 回答标题右侧新增来源标签 | 流式生成时显示绿色 `实时生成中…`；缓存恢复时显示灰色 `来自上次问答` |
| 按钮文案随状态变化 | 没有回答时是「开始回忆」；已有回答（缓存或刚生成）时变成「重新提问」；生成中显示「生成中」 |
| 缓存恢复仍然保持"瞬间出现" | 缓存的意义就是快，不为了"看起来像流式"去做假动画 |

改动文件：`web/src/views/YearlyView.vue`（新增 `memoryFromCache` 状态、`.answer-tag` 样式、按钮文案）。

### 13.5 修复后的验证（公网 + 真实 Chrome）

| 场景 | 结果 |
| --- | --- |
| 首次提问（新账号） | 正文被改写 14 次、10 个不同长度、1176ms 完成 → **确实在逐字写入** |
| 生成完成后 | 不显示缓存标签（它是实时生成的） |
| 刷新页面重进 | **62ms** 出现上次回答，标签显示「来自上次问答」→ 说明这是缓存 |
| 点「重新提问」 | 出现「实时生成中…」标签，正文再次逐字写入（8 个不同长度） |
| 缓存一致性 | 缓存里保存的就是最后一次生成的内容 |

结果：**15/15 通过**。

### 13.6 以后怎么自己确认"流式到底有没有生效"

1. **先强刷新**：电脑上 `Ctrl + F5`；手机上清一下站点缓存或用隐身窗口。旧页面（部署前打开的标签）会一直跑旧 JS。
2. 进入「年度记录」→ 切到目标年份 → 打开「问问这一年」。
3. **必须点一下「开始回忆」/「重新提问」**：只进入页面看到的是缓存，不会调用 AI。
4. 看三处即可判断：
   - 标题旁标签是不是绿色 `实时生成中…`；
   - 按钮是不是变成「生成中」；
   - 正文是不是一个字一个字往上长（约 1～2 秒写完）。
5. 想更硬核一点，按 `F12` → Network → 筛 `stream`：应该能看到一条 `POST /api/ai/yearly-memory/stream`，
   Type 是 `eventsource`/`fetch`，Size 会随着时间增长；如果有这条请求却在最后才一次性显示，
   那才是真被缓冲了（检查 Nginx 的 `proxy_buffering off` 是否生效，或者换一个浏览器/网络再试）。
