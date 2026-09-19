package com.earthonline.backend.ai;

import com.earthonline.backend.ai.dto.YearlyMemoryQueryRequest;
import com.earthonline.backend.ai.dto.YearlyMemoryResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Validated
@RequestMapping("/api/ai/yearly-memory")
public class YearlyMemoryController {
    /** 流式连接最长存活时间，必须大于 AI 客户端读超时（60 秒），否则长回答会被掐断。 */
    private static final long STREAM_TIMEOUT_MS = 180_000L;

    private final YearlyMemoryService service;
    private final TaskExecutor aiStreamExecutor;

    public YearlyMemoryController(YearlyMemoryService service,
                                 @Qualifier("aiStreamExecutor") TaskExecutor aiStreamExecutor) {
        this.service = service;
        this.aiStreamExecutor = aiStreamExecutor;
    }

    @PostMapping("/query")
    public YearlyMemoryResponse query(Authentication authentication,
                                      @Valid @RequestBody YearlyMemoryQueryRequest request) {
        return service.query(authentication.getName(), request.year(), request.question());
    }

    /**
     * 流式提问（SSE）。响应体是 {@code text/event-stream}，事件依次为：
     * {@code meta}（回答依据）→ 多个 {@code delta}（增量文本）→ {@code done}（结束）。
     *
     * 为什么返回 {@link SseEmitter} 而不是 {@code Flux}？因为项目是 Spring MVC（Servlet 栈），
     * 返回 Flux 需要引入 WebFlux 并切换整个 Web 栈，代价远大于收益。
     */
    @PostMapping("/stream")
    public SseEmitter stream(Authentication authentication,
                             @Valid @RequestBody YearlyMemoryQueryRequest request,
                             HttpServletResponse response) {
        String username = authentication.getName();
        // 检索在请求线程里同步做完：这一步失败（比如该年没有任何记录）时响应还没提交，
        // 异常会走全局异常处理器返回正常的 400 JSON；一旦开始写流，HTTP 状态码就定型为 200 了。
        YearlyMemoryStore.Retrieval retrieval = service.prepare(username, request.year(), request.question());
        // 告诉 Nginx 不要缓冲这个响应（Nginx 侧也配了 proxy_buffering off，双保险）
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        aiStreamExecutor.execute(() -> service.stream(username, request.year(), retrieval, emitter));
        return emitter;
    }

    /**
     * 读取上一次的问答。没有缓存时返回 204 No Content，前端据此显示空状态。
     * 这和年度总结的 GET 接口是同一套约定。
     */
    @GetMapping("/answer")
    public ResponseEntity<YearlyMemoryResponse> saved(Authentication authentication,
                                                      @RequestParam @Min(2000) @Max(2100) int year) {
        return service.getSaved(authentication.getName(), year)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
