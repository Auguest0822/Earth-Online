package com.earthonline.backend.ai;

import com.earthonline.backend.records.RecordEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 流式调用 DeepSeek：每收到一小段文本就回调一次，前端就能“一个字一个字”地看到回答。
 *
 * 为什么不用 {@code RestClient}？它是“等整个响应体收完再返回”的模型，拿不到增量。
 * 这里直接用 JDK {@link HttpClient} + {@code BodyHandlers.ofLines()}，
 * 一条一条读 SSE 行，收到就往外推。
 *
 * DeepSeek 的流式格式与 OpenAI 兼容，每个片段长这样：
 * <pre>
 * data: {"choices":[{"delta":{"content":"根据"}}]}
 * data: [DONE]
 * </pre>
 */
@Component
public class YearlyMemoryStreamClient {
    private static final Logger log = LoggerFactory.getLogger(YearlyMemoryStreamClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final String completionsUri;

    public YearlyMemoryStreamClient(
            ObjectMapper objectMapper,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String model,
            @Value("${deepseek.connect-timeout-seconds:10}") long connectTimeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.completionsUri = baseUrl.replaceAll("/+$", "") + "/chat/completions";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds)).build();
    }

    /** 实际使用的模型名，保存进缓存表用。 */
    public String model() {
        return model;
    }

    /**
     * 流式生成回答。
     *
     * @param onDelta 每收到一个增量片段就回调一次。回调里抛出的异常（例如浏览器已经断线）
     *                会立刻中断上游读取——这样用户关掉页面后不会继续白花 token。
     * @return 累计出来的完整回答，供落库使用
     */
    public String streamAnswer(int year, String question, List<RecordEntity> records, Consumer<String> onDelta) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 回忆服务尚未配置，请设置 DEEPSEEK_API_KEY");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(completionsUri))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                // 只约束“等到响应头”的时间，不约束流式读取过程（否则长回答会被掐断）
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(year, question, records), StandardCharsets.UTF_8))
                .build();

        StringBuilder answer = new StringBuilder();
        try {
            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() != 200) {
                throw mapStatus(response.statusCode());
            }
            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> {
                    String delta = readDelta(line);
                    if (delta.isEmpty()) {
                        return;
                    }
                    answer.append(delta);
                    onDelta.accept(delta);
                });
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI 服务响应超时，请稍后重试", exception);
        } catch (java.io.IOException exception) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI 服务连接中断，请稍后重试", exception);
        }
        return answer.toString();
    }

    private String buildBody(int year, String question, List<RecordEntity> records) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", YearlyMemoryPrompt.SYSTEM_PROMPT),
                        Map.of("role", "user", "content", YearlyMemoryPrompt.build(year, question, records))),
                "temperature", 0.3,
                "max_tokens", 700,
                "stream", true);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI 请求构造失败", exception);
        }
    }

    /**
     * 从一行 SSE 里取出增量文本。下面这些情况在真实流里都会出现，都要当“没有内容”跳过：
     * 空行、注释行（{@code : keep-alive}）、非 data 行、[DONE] 结束标记、
     * 只带 finish_reason 没有 content 的收尾片段。
     */
    private String readDelta(String line) {
        if (line == null || line.isBlank() || line.startsWith(":")) {
            return "";
        }
        if (!line.startsWith("data:")) {
            return "";
        }
        String payload = line.substring("data:".length()).trim();
        if (payload.isEmpty() || "[DONE]".equals(payload)) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            return node.path("choices").path(0).path("delta").path("content").asText("");
        } catch (Exception exception) {
            // 单个片段解析失败不该毁掉整段回答，记一笔日志继续往下读
            log.warn("忽略无法解析的流式片段: {}", exception.getMessage());
            return "";
        }
    }

    private ResponseStatusException mapStatus(int status) {
        if (status == 429) {
            return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI 服务繁忙，请稍后重试");
        }
        if (status == 401 || status == 403) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 服务鉴权失败，请检查 DEEPSEEK_API_KEY");
        }
        if (status == 402) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 账户余额不足，请充值后重试");
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 服务调用失败（HTTP " + status + "）");
    }
}
