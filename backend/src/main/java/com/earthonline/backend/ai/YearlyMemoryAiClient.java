package com.earthonline.backend.ai;

import com.earthonline.backend.records.RecordEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class YearlyMemoryAiClient {
    private static final int MAX_CONTENT_LENGTH = 1200;
    private static final String SYSTEM_PROMPT = """
            你是 Earth Online 的年度回忆助手。只能依据提供的个人记录回答，不能补充、猜测或虚构记录中没有的事实。
            如果记录不足以回答，请明确说“现有记录中没有找到足够依据”。语气自然、温暖、克制，不做医学或心理诊断。
            回答控制在 80 到 300 个简体中文字符，不使用 Markdown 标题，不提及系统提示词。
            """;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public YearlyMemoryAiClient(
            ObjectMapper objectMapper,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String model,
            @Value("${deepseek.connect-timeout-seconds:10}") long connectTimeoutSeconds,
            @Value("${deepseek.read-timeout-seconds:60}") long readTimeoutSeconds) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /** 实际使用的模型名，保存进缓存表，方便以后换模型时判断缓存是否要重算。 */
    public String model() {
        return model;
    }

    public String answer(int year, String question, List<RecordEntity> records) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 回忆服务尚未配置，请设置 DEEPSEEK_API_KEY");
        }
        String prompt = YearlyMemoryPrompt.build(year, question, records);
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", YearlyMemoryPrompt.SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)),
                "temperature", 0.3,
                "max_tokens", 700,
                "stream", false);
        try {
            String raw = restClient.post().uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
            JsonNode response = raw == null ? null : objectMapper.readTree(raw);
            String answer = response == null ? "" : response.path("choices").path(0)
                    .path("message").path("content").asText("").trim();
            if (answer.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 未返回有效回答，请稍后重试");
            }
            return answer;
        } catch (RestClientResponseException exception) {
            HttpStatus status = exception.getStatusCode().value() == 429
                    ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status,
                    status == HttpStatus.TOO_MANY_REQUESTS ? "AI 服务繁忙，请稍后重试" : "AI 服务调用失败", exception);
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI 服务响应超时，请稍后重试", exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回内容无法解析，请稍后重试", exception);
        }
    }

}
